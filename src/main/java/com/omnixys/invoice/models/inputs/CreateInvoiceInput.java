package com.omnixys.invoice.models.inputs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateInvoiceInput(
    BigDecimal amount,
    LocalDateTime dueDate,
    UUID issuedBy,
    UUID billedTo
) {
}
