package com.omnixys.invoice.resolvers;

import com.omnixys.invoice.models.dto.PaymentDTO;
import com.omnixys.invoice.models.inputs.CreateInvoiceInput;
import com.omnixys.invoice.models.mapper.InvoiceMapper;
import com.omnixys.invoice.security.CustomUserDetails;
import com.omnixys.invoice.service.InvoiceWriteService;
import com.omnixys.invoice.tracing.LoggerPlus;
import com.omnixys.invoice.tracing.LoggerPlusFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class InvoiceMutationResolver {

    private final InvoiceWriteService invoiceWriteService;
    private final InvoiceMapper invoiceMapper;
    private final LoggerPlusFactory factory;
    private LoggerPlus logger() {
        return factory.getLogger(getClass());
    }

    /**
     * Erstellt eine neue Rechnung.
     */
    @MutationMapping("createInvoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUPREME', 'ELITE', 'BASIC')")
    public UUID createInvoice(
        @Argument("input") final CreateInvoiceInput createInvoiceInput,
        final Authentication authentication
    ) {
        logger().debug("createInvoice: invoiceCreateInput={}", createInvoiceInput);

            final var user = (CustomUserDetails) authentication.getPrincipal();
            final var invoiceInput = invoiceMapper.toInvoice(createInvoiceInput);
            final var id = invoiceWriteService.create(invoiceInput);
//            final var id = invoiceWriteService.create(invoiceInput, user);
            logger().debug("createInvoice: invoiceId={}", id);
            return id;
    }

//    /**
//     * Aktualisiert eine bestehende Rechnung.
//     */
//    @MutationMapping("updateInvoice")
//    public Invoice updateInvoice(
//        @Argument("inputs") final InvoiceUpdateInput invoiceUpdateInput,
//        final Authentication authentication
//    ) {
//        logger().debug("updateInvoice: invoiceUpdateInput={}", invoiceUpdateInput);
//
//        try {
//            final var user = (CustomUserDetails) authentication.getPrincipal();
//            final var versionResult = writeService.update(invoiceUpdateInput, user);
//            logger().debug("updateInvoice: versionResult={}", versionResult);
//            return versionResult;
//        } catch (Exception ex) {
//            logger().error("updateInvoice failed: {}", ex.getMessage());
//            throw new BadUserInputException("Fehler beim Aktualisieren der Rechnung.");
//        }
//    }

//    /**
//     * Löscht eine Rechnung eines Kunden.
//     */
//    @MutationMapping("deleteInvoice")
//    public boolean deleteInvoice(
//        @Argument("customerId") final UUID customerId,
//        final Authentication authentication
//    ) {
//        logger().debug("deleteInvoice: customerId={}", customerId);
//
//        try {
//            final var user = (CustomUserDetails) authentication.getPrincipal();
//            return invoiceService.delete(customerId, user);
//        } catch (Exception ex) {
//            logger().error("deleteInvoice failed: {}", ex.getMessage());
//            throw new BadUserInputException("Fehler beim Löschen der Rechnung.");
//        }
//    }

    /**
     * Führt eine Zahlung für eine bestimmte Rechnung durch.
     * <p>
     * Diese Mutation verarbeitet eine Zahlung, indem sie die Zahlungsdaten aus dem übergebenen DTO
     * entgegennimmt und die Zahlung im zugehörigen Rechnungsservice ausführt.
     * Der authentifizierte Benutzer wird aus dem aktuellen Sicherheitskontext extrahiert.
     * </p>
     *
     * @param paymentDTO     Das Zahlungsobjekt mit Rechnungs-ID und Betrag
     * @param authentication Die Authentifizierung des aktuell eingeloggten Benutzers
     * @return Die ID der verarbeiteten Zahlung
     */
    @MutationMapping("makePayment")
    public BigDecimal makePayment(
        @Argument("input") @Valid final PaymentDTO paymentDTO,
        final Authentication authentication
    ) {
        logger().debug("makePayment: invoiceId={}, amount={}", paymentDTO.invoiceId(), paymentDTO.amount());
        final var user = (CustomUserDetails) authentication.getPrincipal();
        return invoiceWriteService.pay(paymentDTO, user);
    }
}
