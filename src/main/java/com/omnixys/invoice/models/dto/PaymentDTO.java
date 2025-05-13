package com.omnixys.invoice.models.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object für Zahlungsinformationen.
 * <p>
 * Dieses DTO wird verwendet, um eine Zahlung für eine Rechnung durchzuführen.
 * Es enthält die eindeutige ID der Rechnung sowie den Betrag, der gezahlt werden soll.
 * </p>
 */
public record PaymentDTO(

    /**
     * Die eindeutige ID der Rechnung, auf die sich die Zahlung bezieht.
     */
    @NotNull(message = "Rechnungs-ID darf nicht null sein.")
    UUID invoiceId,

    /**
     * Der zu zahlende Betrag.
     * <p>
     * Der Betrag muss größer als null sein.
     * </p>
     */
    @NotNull(message = "Zahlbetrag darf nicht null sein.")
    @Positive(message = "Zahlbetrag muss positiv sein.")
    BigDecimal amount,

    BigDecimal alreadyPaid

) {}
