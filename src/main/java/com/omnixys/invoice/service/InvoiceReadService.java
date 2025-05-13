package com.omnixys.invoice.service;

import com.omnixys.invoice.exception.AccessForbiddenException;
import com.omnixys.invoice.exception.NotFoundException;
import com.omnixys.invoice.messaging.KafkaPublisherService;
import com.omnixys.invoice.models.dto.PaymentDTO;
import com.omnixys.invoice.models.entitys.Invoice;
import com.omnixys.invoice.models.enums.InfoType;
import com.omnixys.invoice.models.enums.StatusType;
import com.omnixys.invoice.models.payload.InfoPayload;
import com.omnixys.invoice.repository.InvoiceRepository;
import com.omnixys.invoice.repository.SpecificationBuilder;
import com.omnixys.invoice.security.CustomUserDetails;
import com.omnixys.invoice.security.enums.RoleType;
import com.omnixys.invoice.tracing.LoggerPlus;
import com.omnixys.invoice.tracing.LoggerPlusFactory;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.graphql.client.FieldAccessException;
import org.springframework.graphql.client.GraphQlTransportException;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.omnixys.invoice.security.enums.RoleType.ADMIN;
import static com.omnixys.invoice.security.enums.RoleType.USER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InvoiceReadService {
    private final InvoiceRepository invoiceRepository;
    private final HttpGraphQlClient graphQlClient;
    private final SpecificationBuilder specificationBuilder;
    private final Tracer tracer;
    private final LoggerPlusFactory factory;
    private LoggerPlus logger() {
        return factory.getLogger(getClass());
    }

    /**
     * Findet eine Rechnung anhand der ID.
     *
     * @param id   Die UUID der Rechnung
     * @param user Der angemeldete Benutzer
     * @return Die gefundene Rechnung
     * @throws NotFoundException Wenn die Rechnung nicht existiert
     * @throws AccessForbiddenException Wenn der Benutzer keine Berechtigung hat
     */
    @Observed(name = "invoice-service.read.find-by-id")
    public @NonNull Invoice findById(final UUID id, final CustomUserDetails user) {
        Span serviceSpan = tracer.spanBuilder("invoice-service.read.find-by-id").startSpan();
        try (Scope serviceScope = serviceSpan.makeCurrent()) {
            assert serviceScope != null;
            logger().debug("findById: id={} user={}", id, user);
            final var invoice = invoiceRepository.findById(id).orElseThrow(NotFoundException::new);
            //validateUserRole(user, invoice);
            logger().debug("findById: Invoice={}", invoice);
            return invoice;
        } catch (Exception e) {
            serviceSpan.recordException(e);
            serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            serviceSpan.end();
        }
    }

    /**
     * Findet alle Rechnungen, wenn der Benutzer berechtigt ist.
     *
     * @param user Der Benutzer
     * @return Sammlung von Rechnungen
     * @throws AccessForbiddenException Wenn der Benutzer keine Berechtigung hat
     */
    @Observed(name = "invoice-service.read.find")
    public @NonNull Collection<Invoice> find(final Map<String, List<Object>> searchCriteria, final UserDetails user) {
        Span serviceSpan = tracer.spanBuilder("invoice-service.read.find").startSpan();
        try (Scope serviceScope = serviceSpan.makeCurrent()) {
            assert serviceScope != null;
            validateUserRole(user);

            if (searchCriteria.isEmpty()) {
                return invoiceRepository.findAll();
            }

            final var specification = specificationBuilder
                .build(searchCriteria)
                .orElseThrow(() -> new NotFoundException(searchCriteria));
            final var invoices = invoiceRepository.findAll(specification);

            if (invoices.isEmpty()) {
                throw new NotFoundException(searchCriteria);
            }

            logger().debug("find: invoices={}", invoices);
            return invoices;
        } catch (Exception e) {
            serviceSpan.recordException(e);
            serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            serviceSpan.end();
        }
    }

    @Observed(name = "invoice-service.read.find-by-customer")
    public @NonNull List<Invoice> findByCustomer(final UUID customerId, final Map<String, List<Object>> searchCriteria, final CustomUserDetails user) {
        Span span = tracer.spanBuilder("invoice-service.read.find-by-customer").startSpan();
        try (Scope scope = span.makeCurrent()) {
            assert scope != null;
            logger().debug("findByCustomer: customerId={}, user={}", customerId, user);

            // Zugriffsschutz: Nur eigene Rechnungen oder Admin
//            if (!user.getPersonId().equals(customerId) && !user.hasRole(ADMIN)) {
//                throw new AccessForbiddenException("Zugriff auf fremde Rechnungen nicht erlaubt");
//            }

            // Falls kein Filter: direkte Suche nach Beteiligung
            if (searchCriteria.isEmpty()) {
                return invoiceRepository.findByIssuedByOrBilledTo(customerId, customerId);
            }

            // ODER-Spezifikation: issuedBy == id ODER billedTo == id
            final var orSpec = specificationBuilder
                .buildOr(Map.of(
                    "issuedBy", List.of(customerId),
                    "billedTo", List.of(customerId)
                ), List.of("issuedBy", "billedTo"))
                .orElseThrow(() -> new NotFoundException("Kein Zugriff auf Rechnungen."));

            // UND-Spezifikation aus den eigentlichen Kriterien
            final var andSpec = specificationBuilder
                .build(searchCriteria)
                .orElseThrow(() -> new NotFoundException(searchCriteria));

            final var fullSpec = Specification.where(orSpec).and(andSpec);
            final var invoices = invoiceRepository.findAll(fullSpec);

            if (invoices.isEmpty()) {
                throw new NotFoundException(searchCriteria);
            }

            logger().debug("findByCustomer: invoices={}", invoices);
            return invoices;
        } finally {
            span.end();
        }
    }


    /**
     * Berechnet Gesamtinformationen für Rechnungen anhand des Typs.
     *
     * @param infoType   Der Typ der Information (PAYMENTS oder INVOICE).
     * @param statusType Der Rechnungsstatus.
     * @param token      JWT-Token für die Authentifizierung.
     * @return InfoPayload mit Gesamtanzahl und Gesamtbetrag.
     */
    @Observed(name = "invoice-service.read.total-info")
    public InfoPayload totalInfo(final boolean isIssuer, final UUID personId, final InfoType infoType, final String statusType, final String token) {
        Span serviceSpan = tracer.spanBuilder("invoice-service.read.total-info").startSpan();
        try (Scope serviceScope = serviceSpan.makeCurrent()) {
            assert serviceScope != null;
            logger().debug("totalInfo: isIssuer={} personId={} infoType={} status={}", isIssuer, personId, infoType, statusType);

            final var status = StatusType.valueOf(statusType);

            List<Invoice> invoices = isIssuer
                ? invoiceRepository.findByIssuedByAndStatus(personId, status)
                : invoiceRepository.findByBilledToAndStatus(personId, status);


            return switch (infoType) {
                case PAYMENTS -> calculatePaymentInfo(invoices, token);
                case INVOICES -> calculateInvoiceInfo(invoices);
            };
        } catch (Exception e) {
            serviceSpan.recordException(e);
            serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            serviceSpan.end();
        }
    }

    @Observed(name = "invoice-service.read.info-by-customer")
    public InfoPayload infoByCustomer(UUID customerId, InfoType infoType, String statusType, CustomUserDetails user) {
        Span span = tracer.spanBuilder("invoice-service.read.info-by-customer").startSpan();
        try (Scope scope = span.makeCurrent()) {
            assert scope != null;
            logger().debug("infoByCustomer: customerId={}, infoType={}", customerId, infoType);

            StatusType status = null;
            if (statusType != null && !statusType.isBlank()) {
                status = StatusType.valueOf(statusType);
            }

            final var invoices = invoiceRepository.findByIssuedByOrBilledToAndOptionalStatus(customerId, customerId, status);

            return switch (infoType) {
                case INVOICES -> calculateInvoiceInfo(invoices);
                case PAYMENTS -> calculatePaymentInfo(invoices, user.getToken());
            };
        } finally {
            span.end();
        }
    }

    /**
     * Berechnet die Zahlungsinformationen für eine gegebene Rechnung.
     * <p>
     * Diese Methode lädt alle bisherigen Zahlungen zur angegebenen Rechnung und berechnet die
     * Gesamtanzahl sowie den Gesamtbetrag. Die Zahlungsinformationen werden über eine externe
     * GraphQL-API anhand der Zahlungs-IDs geladen.
     * </p>
     *
     * @param invoiceId Die UUID der Rechnung
     * @param token     Das JWT-Token zur Authentifizierung gegenüber der externen GraphQL-API
     * @return Ein {@link InfoPayload} mit Anzahl und Gesamtsumme der Zahlungen
     * @throws NotFoundException Falls die Rechnung nicht existiert oder die Zahlungsabfrage fehlschlägt
     */
    @Observed(name = "invoice-service.read.payment-info")
    public InfoPayload paymentInfo(final UUID invoiceId, final String token) {
        Span serviceSpan = tracer.spanBuilder("invoice-service.read.payment-info").startSpan();
        try (Scope serviceScope = serviceSpan.makeCurrent()) {
            assert serviceScope != null;
            logger().debug("paymentInfo: invoiceId={}", invoiceId);

            Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException(invoiceId));

            logger().debug("paymentInfo: invoice={}", invoice);

            List<PaymentDTO> payments = invoice.getPayments() != null
                ? fetchPayments(invoice.getPayments(), token)
                : Collections.emptyList();

            logger().debug("paymentInfo: payments={}", payments);

            BigDecimal totalAmount = payments.stream()
                .map(PaymentDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new InfoPayload(payments.size(), totalAmount);
        } catch (Exception e) {
            serviceSpan.recordException(e);
            serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            serviceSpan.end();
        }
    }

    /**
     * Berechnet die Gesamtanzahl und Summe aller Rechnungen.
     *
     * @param invoices Liste der Rechnungen.
     * @return InfoPayload mit Gesamtanzahl und Gesamtbetrag.
     */
    private InfoPayload calculateInvoiceInfo(List<Invoice> invoices) {
        int count = invoices.size();
        BigDecimal totalAmount = invoices.stream()
            .map(Invoice::getAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InfoPayload(count, totalAmount);
    }

    /**
     * Berechnet die Gesamtanzahl und Summe aller Zahlungen in Rechnungen.
     *
     * @param invoices Liste der Rechnungen.
     * @param token    JWT-Token für die GraphQL-Abfrage.
     * @return InfoPayload mit Gesamtanzahl und Gesamtbetrag.
     */
    private InfoPayload calculatePaymentInfo(List<Invoice> invoices, String token) {
        List<UUID> paymentIds = invoices.stream()
            .map(Invoice::getPayments)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .toList();

        List<PaymentDTO> payments = fetchPayments(paymentIds, token);

        BigDecimal totalAmount = payments.stream()
            .map(PaymentDTO::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InfoPayload(payments.size(), totalAmount);
    }

    /**
     * Berechnet die Gesamtanzahl und Summe aller Zahlungen einer Rechnung.
     *
     * @param invoiceId Die UUID der Rechnung
     * @return InfoPayload mit Gesamtanzahl und Gesamtbetrag
     */
//    private InfoPayload calculatePaymentInfo(final UUID invoiceId, final String token) {
//        logger().debug("calculatePaymentInfo: invoiceId={}", invoiceId);
//
//        Invoice invoice = invoiceRepository.findById(invoiceId)
//            .orElseThrow(() -> new NotFoundException(invoiceId));
//
//        List<UUID> paymentIds = Optional.ofNullable(invoice.getPayments()).orElse(Collections.emptyList());
//        List<PaymentDTO> payments = fetchPayments(paymentIds, token);
//
//        BigDecimal totalAmount = payments.stream()
//            .map(PaymentDTO::amount)
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        return new InfoPayload(payments.size(), totalAmount);
//    }

    /**
     * Berechnet die Gesamtanzahl und Summe aller Rechnungen eines Kunden.
     *
     * @param username Die UUID des Kunden
     * @return InfoPayload mit Gesamtanzahl und Gesamtbetrag
     */
//    private InfoPayload calculateTotalInfo(final String username, final String token) {
//        logger().debug("calculateTotalInfo: username={}", username);
//
//        List<Invoice> invoices = invoiceRepository.findByUsername(username);
//
//        if (invoices.isEmpty()) {
//            return new InfoPayload(0, BigDecimal.ZERO);
//        }
//
//        int count = invoices.stream()
//            .mapToInt(invoice -> invoice.getPayments() != null ? invoice.getPayments().size() : 0)
//            .sum();
//
//        BigDecimal totalAmount = invoices.stream()
//            .flatMap(invoice -> invoice.getPayments() != null ? fetchPayments(invoice.getPayments(), token).stream() : Stream.empty())
//            .map(PaymentDTO::amount)
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        return new InfoPayload(count, totalAmount);
//    }


    //TODO PaymentReadService
    /**
     * Ruft die Zahlungsdetails zu einer Liste von Zahlungs-IDs über eine externe GraphQL-API ab.
     * <p>
     * Die Methode verwendet ein JWT für die Authentifizierung und erwartet eine Antwortstruktur,
     * die den Feldern {@code id}, {@code amount} und {@code created} entspricht.
     * </p>
     *
     * @param paymentIds Die Liste von Zahlungs-UUIDs
     * @param token      Das JWT-Token zur Authentifizierung gegenüber dem externen Service
     * @return Eine Liste von {@link PaymentDTO} mit den abgefragten Zahlungsdaten
     * @throws NotFoundException Wenn die Anfrage fehlschlägt oder keine Daten gefunden werden
     */
    private List<PaymentDTO> fetchPayments(final List<UUID> paymentIds, final String token) {
        logger().debug("fetchPayments: ids={}", paymentIds);

        final String query = """
        query Payments($id: [ID!]) {
            payments(ids: $id) {
                id
                amount
                created
            }
        }
        """;

        final Map<String, Object> variables = Map.of("id", paymentIds);

        try {
            return graphQlClient
                .mutate()
                .header(AUTHORIZATION, token)
                .build()
                .document(query)
                .variables(variables)
                .retrieveSync("payments")
                .toEntityList(PaymentDTO.class);
        } catch (final FieldAccessException | GraphQlTransportException ex) {
            logger().error("fetchPayments error", ex);
            throw new NotFoundException("Zahlungen konnten nicht abgerufen werden.");
        }
    }

//TODO UtilService
    /**
     * Überprüft, ob der Benutzer eine gültige Rolle hat.
     *
     * @param user Der Benutzer
     * @throws AccessForbiddenException Falls keine Berechtigung vorliegt
     */
    public void validateUserRole(UserDetails user) {
        final var roles = user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(str -> str.substring(RoleType.ROLE_PREFIX.length()))
            .map(RoleType::valueOf)
            .collect(Collectors.toSet());

        if (!roles.contains(ADMIN) && !roles.contains(USER)) {
            throw new AccessForbiddenException(user.getUsername(), roles);
        }
    }

//    public void validateUserRole(final UserDetails user, final Invoice invoice) {
//        final var roles = user.getAuthorities().stream()
//            .map(GrantedAuthority::getAuthority)
//            .map(str -> str.substring(RoleType.ROLE_PREFIX.length()))
//            .map(RoleType::valueOf)
//            .collect(Collectors.toSet());
//
//
//        if (!roles.contains(ADMIN) && !roles.contains(USER)) {
//            throw new AccessForbiddenException(user.getUsername(), roles);
//        }
//    }
}
