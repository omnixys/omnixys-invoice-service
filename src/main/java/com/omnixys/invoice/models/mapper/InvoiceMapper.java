package com.omnixys.invoice.models.mapper;

import com.omnixys.invoice.models.entitys.Invoice;
import com.omnixys.invoice.models.inputs.CreateInvoiceInput;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface InvoiceMapper {
    Invoice toInvoice(CreateInvoiceInput createInvoiceInput);
}
