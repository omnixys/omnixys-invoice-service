package com.omnixys.invoice.service;

import com.omnixys.invoice.exception.NotFoundException;
import com.omnixys.invoice.messaging.KafkaPublisherService;
import com.omnixys.invoice.models.dto.NewPaymentIdDTO;
import com.omnixys.invoice.models.dto.PaymentDTO;
import com.omnixys.invoice.models.entitys.Invoice;
import com.omnixys.invoice.models.enums.StatusType;
import com.omnixys.invoice.repository.InvoiceRepository;
import com.omnixys.invoice.security.CustomUserDetails;
import com.omnixys.invoice.tracing.LoggerPlus;
import com.omnixys.invoice.tracing.LoggerPlusFactory;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.omnixys.invoice.models.enums.StatusType.PENDING;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceWriteService {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\"\\d{1,3}\"$");

    private final InvoiceRepository invoiceRepository;
    private final InvoiceReadService invoiceReadService;
    private final Tracer tracer;
    private final KafkaPublisherService kafkaPublisherService;
    private final LoggerPlusFactory factory;
    private LoggerPlus logger() {
        return factory.getLogger(getClass());
    }


    @Observed(name = "invoice-service.write.create")
    public UUID create(final Invoice invoice, final CustomUserDetails user) {
        Span serviceSpan = tracer.spanBuilder("invoice-service.write.create").startSpan();
        try (Scope serviceScope = serviceSpan.makeCurrent()) {
            assert serviceScope != null;
            logger().debug("create: invoice={}", invoice);
            invoice.setStatus(PENDING);
            // readService.validateUserRole(user);
            final var newInvoice = invoiceRepository.save(invoice);
            logger().debug("create: newInvoice={}", newInvoice);
            return newInvoice.getId();
        } catch (Exception e) {
            serviceSpan.recordException(e);
            serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            serviceSpan.end();
        }
    }

    @Observed(name = "invoice-service.write.update")
    public Invoice update(final Invoice invoice, final UUID id, final CustomUserDetails userDetails) {
        Span serviceSpan = tracer.spanBuilder("account-service.write.update").startSpan();
        try (Scope serviceScope = serviceSpan.makeCurrent()) {
            assert serviceScope != null;
            logger().debug("update: invoice={}, id={}", invoice, id);
            Invoice updatedInvoice = invoiceRepository.save(invoice);

            logger().debug("update: updatedInvoice={}", updatedInvoice);
            return updatedInvoice;
        } catch (Exception e) {
            serviceSpan.recordException(e);
            serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            serviceSpan.end();
        }
    }

    /**
     * Führt eine Zahlung für eine Rechnung durch.
     * <p>
     * Es wird geprüft, ob die Rechnung bereits bezahlt ist. Falls nicht, wird berechnet, wie viel noch offen ist.
     * Der tatsächlich gezahlte Betrag ergibt sich aus dem Minimum von Restbetrag und angegebenem Zahlungsbetrag.
     * </p>
     *
     * @param paymentDTO Das Zahlungsobjekt mit Rechnungs-ID, Betrag und bereits gezahltem Betrag
     * @param user       Der aktuell authentifizierte Benutzer
     * @return Der tatsächlich verarbeitete Zahlungsbetrag
     * @throws IllegalStateException Wenn die Rechnung bereits bezahlt ist
     */
    @Observed(name = "invoice-service.write.pay")
    public BigDecimal pay(final PaymentDTO paymentDTO, final CustomUserDetails user) {
        Span serviceSpan = tracer.spanBuilder("invoice-service.write.pay").startSpan();
        try (Scope serviceScope = serviceSpan.makeCurrent()) {
            assert serviceScope != null;
            logger().debug("pay: paymentDTO={}", paymentDTO);

            final UUID invoiceId = paymentDTO.invoiceId();
            final BigDecimal paymentAmount = paymentDTO.amount();

            Invoice invoice = invoiceReadService.findById(invoiceId, user);

            if (invoice.getStatus() == StatusType.PAID) {
                throw new IllegalStateException("Rechnung wurde bereits bezahlt.");
            }

            BigDecimal alreadyPaid = paymentDTO.alreadyPaid(); // vom Aufrufer berechnet
            BigDecimal remainingAmount = invoice.getAmount().subtract(alreadyPaid);
            logger().debug("pay: remainingAmount={}", remainingAmount);

            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Rechnung wurde bereits vollständig bezahlt.");
            }

            BigDecimal paidNow = paymentAmount.min(remainingAmount);
            logger().debug("pay: paidNow={}", paidNow);

            if (paidNow.compareTo(remainingAmount) >= 0) {
                invoice.setStatus(StatusType.PAID);
                invoiceRepository.save(invoice);
                logger().info("Rechnung {} wurde vollständig bezahlt.", invoiceId);
            } else {
                logger().info("Teilzahlung erhalten: {} von {} noch offen.", paidNow, remainingAmount);
            }

            return paidNow;
        } catch (Exception e) {
            serviceSpan.recordException(e);
            serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            serviceSpan.end();
        }
    }

    @Observed(name = "invoice-service.write.finalize-payment")
    public void finalizePayment(NewPaymentIdDTO newPaymentIdDTO) {
        Span serviceSpan = tracer.spanBuilder("invoice-service.write.finalize-payment").startSpan();
        try (Scope serviceScope = serviceSpan.makeCurrent()) {
            assert serviceScope != null;
            logger().debug("finalizePayment: newPaymentIdDTO={}", newPaymentIdDTO);
            final var invoice = invoiceRepository.findById(newPaymentIdDTO.invoiceId()).orElseThrow(() -> new NotFoundException(newPaymentIdDTO.invoiceId()));
            invoice.addPayment(newPaymentIdDTO.paymentId()); // sicher!
            final var newInvoice = invoiceRepository.save(invoice);
            logger().debug("finalizePayment: newInvoice={}", newInvoice);
        } catch (Exception e) {
            serviceSpan.recordException(e);
            serviceSpan.setAttribute("exception.class", e.getClass().getSimpleName());
            throw e;
        } finally {
            serviceSpan.end();
        }
    }


//    @KafkaListener(topics = "balance-response", groupId = "invoice-service")
//    public void listenForBalanceResponse(String message) {
//        logger().debug("Received balance-response: {}", message);
//        this.balance = Double.parseDouble(message);
//    }
}
