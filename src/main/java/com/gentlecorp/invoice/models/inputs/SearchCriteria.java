package com.gentlecorp.invoice.models.inputs;

import com.gentlecorp.invoice.models.enums.InfoType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SearchCriteria(
    InfoType infoType,
    String status,
    String username,
    LocalDateTime dueDate,
    LocalDateTime created
) {
    /**
     * Konvertiert die Kriterien in eine Map für die Filterung.
     *
     * @return Eine Map mit den gesetzten Suchkriterien.
     */
    public Map<String, List<Object>> toMap() {
        final Map<String, List<Object>> map = new HashMap<>();

        if (infoType != null) {
            map.put("infoType", List.of(infoType));
        }
        if (status != null) {
            map.put("status", List.of(status));
        }
        if (username != null) {
            map.put("customerId", List.of(username));
        }
        if (dueDate != null) {
            map.put("dueDate", List.of(dueDate));
        }
        if (created != null) {
            map.put("created", List.of(created));
        }

        return map;
    }
}
