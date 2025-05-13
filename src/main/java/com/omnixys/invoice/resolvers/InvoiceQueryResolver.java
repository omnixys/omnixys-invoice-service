package com.omnixys.invoice.resolvers;

import com.omnixys.invoice.exception.AccessForbiddenException;
import com.omnixys.invoice.exception.NotFoundException;
import com.omnixys.invoice.models.entitys.Invoice;
import com.omnixys.invoice.models.enums.InfoType;
import com.omnixys.invoice.models.enums.StatusType;
import com.omnixys.invoice.models.inputs.InfoInput;
import com.omnixys.invoice.models.inputs.SearchCriteria;
import com.omnixys.invoice.models.payload.InfoPayload;
import com.omnixys.invoice.security.CustomUserDetails;
import com.omnixys.invoice.service.InvoiceReadService;
import com.omnixys.invoice.tracing.LoggerPlus;
import com.omnixys.invoice.tracing.LoggerPlusFactory;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static org.springframework.graphql.execution.ErrorType.FORBIDDEN;
import static org.springframework.graphql.execution.ErrorType.NOT_FOUND;

@Controller
@RequiredArgsConstructor
public class InvoiceQueryResolver {

    private final InvoiceReadService invoiceReadService;
    private final LoggerPlusFactory factory;
    private LoggerPlus logger() {
        return factory.getLogger(getClass());
    }

    @QueryMapping("invoiceById")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUPREME', 'ELITE', 'BASIC')")
    Invoice getById(
        @Argument("id") final UUID id,
        final Authentication authentication
    ) {
        logger().debug("getById: id={}", id);

        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var invoice = invoiceReadService.findById(id, user);

        logger().debug("getById: invoice={}", invoice);
        return invoice;
    }

    @QueryMapping("invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    Collection<Invoice> getInvoices(
        @Argument("searchCriteria") final Optional<SearchCriteria> input,
        final Authentication authentication
    ) {
        logger().debug("getInvoices: inputs={}", input);

        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var searchCriteria = input.map(SearchCriteria::toMap).orElse(emptyMap());
        final var invoices = invoiceReadService.find(searchCriteria, user);
        logger().debug("getInvoices: Invoices={}", invoices);
        return invoices;
    }

    @QueryMapping("invoicesByCustomer")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUPREME', 'ELITE', 'BASIC')")
    Collection<Invoice> getInvoicesByCustomer(
        @Argument("customerId") UUID customerId,
        @Argument("searchCriteria") final Optional<SearchCriteria> input,
        final Authentication authentication
    ) {
        logger().debug("getInvoicesByCustomer: customerId={}, searchCriteria={}", customerId, input);

        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var searchCriteria = input.map(SearchCriteria::toMap).orElse(emptyMap());
        final var invoices = invoiceReadService.findByCustomer(customerId, searchCriteria, user);
        logger().debug("getInvoicesByCustomer: Invoices={}", invoices);
        return invoices;
    }

    @QueryMapping("totalInvoicesInfo")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    InfoPayload totalInvoiceInfo(
        @Argument("role") final boolean isIssuer,
        @Argument("personId") final UUID personId,
        @Argument("infoType") final InfoType infoType,
        @Argument("status") final String statusType,
        final Authentication authentication
    ) {
        logger().debug("totalInvoiceInfo: isIssuer={}, personId={}, infoType={}, statusType={}", isIssuer, personId, infoType, statusType);

        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var payload = invoiceReadService.totalInfo(isIssuer, personId, infoType, statusType, user.getToken());
        logger().debug("totalInvoiceInfo: payload={}", payload);
        return payload;
    }

    @QueryMapping("invoiceInfoByCustomer")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUPREME', 'ELITE', 'BASIC')")
    InfoPayload invoiceInfoByCustomer(
        @Argument("customerId") UUID customerId,
        @Argument("infoType") InfoType infoType,
        @Argument("status") final String statusType,
        final Authentication authentication
    ) {
        logger().debug("invoiceInfoByCustomer: infoType={}, statusType={}", infoType, statusType);

        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var payload = invoiceReadService.infoByCustomer(customerId, infoType, statusType, user);
        logger().debug("invoiceInfoByCustomer: payload={}", payload);
        return payload;
    }

    @QueryMapping("paymentInfo")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'SUPREME', 'ELITE', 'BASIC')")
    InfoPayload getPaymentInfo(
        @Argument("invoiceId") final UUID id,
        final Authentication authentication
    ) {
        logger().debug("addContact: id={}", id);

        final var user = (CustomUserDetails) authentication.getPrincipal();
        final var payload = invoiceReadService.paymentInfo(id, user.getToken());

        logger().debug("addContact: payload={}", payload);
        return payload;
    }

    /**
     * Behandelt eine `AccessForbiddenException` und gibt ein entsprechendes GraphQL-Fehlerobjekt zurück.
     *
     * @param ex Die ausgelöste Ausnahme.
     * @param env Das GraphQL-Umfeld für Fehlerinformationen.
     * @return Ein `GraphQLError` mit der Fehlerbeschreibung.
     */
    @GraphQlExceptionHandler
    GraphQLError onAccessForbidden(final AccessForbiddenException ex, DataFetchingEnvironment env) {
        logger().error("onAccessForbidden: {}", ex.getMessage());
        return GraphQLError.newError()
            .errorType(FORBIDDEN)
            .message(ex.getMessage())
            .path(env.getExecutionStepInfo().getPath().toList()) // Dynamischer Query-Pfad
            .location(env.getExecutionStepInfo().getField().getSingleField().getSourceLocation()) // GraphQL Location
            .build();
    }

    /**
     * Behandelt eine `NotFoundException` und gibt ein entsprechendes GraphQL-Fehlerobjekt zurück.
     *
     * @param ex Die ausgelöste Ausnahme.
     * @param env Das GraphQL-Umfeld für Fehlerinformationen.
     * @return Ein `GraphQLError` mit der Fehlerbeschreibung.
     */
    @GraphQlExceptionHandler
    GraphQLError onNotFound(final NotFoundException ex, DataFetchingEnvironment env) {
        logger().error("onNotFound: {}", ex.getMessage());
        return GraphQLError.newError()
            .errorType(NOT_FOUND)
            .message(ex.getMessage())
            .path(env.getExecutionStepInfo().getPath().toList()) // Dynamischer Query-Pfad
            .location(env.getExecutionStepInfo().getField().getSingleField().getSourceLocation()) // GraphQL Location
            .build();
    }
}
