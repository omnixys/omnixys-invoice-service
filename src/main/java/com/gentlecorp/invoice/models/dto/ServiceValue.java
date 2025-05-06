package com.gentlecorp.invoice.models.dto;

public record ServiceValue(
    String schema,
    String host,
    int port
) {
}
