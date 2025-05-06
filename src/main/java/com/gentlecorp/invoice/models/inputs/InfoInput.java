package com.gentlecorp.invoice.models.inputs;

import com.gentlecorp.invoice.models.enums.InfoType;

import java.util.UUID;

public record InfoInput(
    InfoType infoType,
    String username,
    boolean doTotalInfo,
    UUID invoiceId
) {
}
