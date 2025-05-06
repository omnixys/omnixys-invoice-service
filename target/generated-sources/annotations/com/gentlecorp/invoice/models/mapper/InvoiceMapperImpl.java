package com.gentlecorp.invoice.models.mapper;

import com.gentlecorp.invoice.models.entitys.Invoice;
import com.gentlecorp.invoice.models.inputs.CreateInvoiceInput;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-05T23:00:57+0200",
    comments = "version: 1.6.0.Beta2, compiler: javac, environment: Java 23.0.2 (Homebrew)"
)
@Component
public class InvoiceMapperImpl implements InvoiceMapper {

    @Override
    public Invoice toInvoice(CreateInvoiceInput createInvoiceInput) {
        if ( createInvoiceInput == null ) {
            return null;
        }

        Invoice.InvoiceBuilder invoice = Invoice.builder();

        invoice.amount( createInvoiceInput.amount() );
        invoice.dueDate( createInvoiceInput.dueDate() );
        invoice.username( createInvoiceInput.username() );

        return invoice.build();
    }
}
