package com.omnixys.invoice.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.stream.Stream;

public enum StatusType {
    PENDING("PND"),
    PAID("P"),
    OVERDUE("O");

    private final String status;

    StatusType(final String status) {
        this.status = status;
    }

    @JsonValue
    private String getStatus() {
        return status;
    }

    @JsonCreator
    public static StatusType of(final String value) {
        return Stream.of(values())
            .filter(statusType -> statusType.status.equalsIgnoreCase(value) || statusType.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(value));
    }
}
