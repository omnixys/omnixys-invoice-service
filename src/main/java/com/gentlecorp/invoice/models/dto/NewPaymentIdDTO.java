package com.gentlecorp.invoice.models.dto;

import java.util.UUID;

public record NewPaymentIdDTO(
    UUID paymentId,
    UUID invoiceId
) {
}
