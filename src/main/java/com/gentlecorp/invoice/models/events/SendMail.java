package com.omnixys.invoice.models.events;

import com.omnixys.invoice.models.entitys.Invoice;

import java.util.UUID;

public record SendMail(
    UUID id
) {
    public static SendMail fromEntity(final Invoice invoice) {
        return new SendMail(
            invoice.getId()
        );
    }
}