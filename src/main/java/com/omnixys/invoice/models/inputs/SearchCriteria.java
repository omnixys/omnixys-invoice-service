package com.omnixys.invoice.models.inputs;

import com.omnixys.invoice.models.enums.InfoType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SearchCriteria(
    InfoType infoType,
    String status,
    UUID issuedBy,
    UUID billedTo,
    LocalDateTime dueBefore,
    LocalDateTime dueAfter,
    BigDecimal minAmount,
    BigDecimal maxAmount
) {
    /**
     * Konvertiert die Kriterien in eine Map für die Filterung.
     *
     * @return Eine Map mit den gesetzten Suchkriterien.
     */
    public Map<String, List<Object>> toMap() {
        final Map<String, List<Object>> map = new HashMap<>();
        if (infoType != null) map.put("infoType", List.of(infoType));
        if (status != null) map.put("status", List.of(status));
        if (issuedBy != null) map.put("issuedBy", List.of(issuedBy));
        if (billedTo != null) map.put("billedTo", List.of(billedTo));
        if (dueBefore != null) map.put("dueBefore", List.of(dueBefore));
        if (dueAfter != null) map.put("dueAfter", List.of(dueAfter));
        if (minAmount != null) map.put("minAmount", List.of(minAmount));
        if (maxAmount != null) map.put("maxAmount", List.of(maxAmount));
        return map;
    }
}
