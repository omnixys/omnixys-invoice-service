package com.omnixys.invoice.models.inputs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateInvoiceInput(
    BigDecimal amount,
    LocalDateTime dueDate,
    String username
) {
}
