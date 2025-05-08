package com.omnixys.invoice.models.inputs;

import com.omnixys.invoice.models.enums.InfoType;

import java.util.UUID;

public record InfoInput(
    InfoType infoType,
    String username,
    boolean doTotalInfo,
    UUID invoiceId
) {
}
