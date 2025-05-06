package com.gentlecorp.invoice.util;

import com.gentlecorp.invoice.models.dto.ServiceValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.class);

    public static final String PROBLEM_PATH = "/problem";
    public static final String GRAPHQL_ENDPOINT = "/graphql";

    public static final String ID_PATTERN = "[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}";
    public static final String VERSION_NUMBER_MISSING = "Versionsnummer fehlt";

    private static final String PAYMENT_SCHEMA_ENV = System.getenv("PAYMENT_SERVICE_SCHEMA");
    private static final String PAYMENT_HOST_ENV = System.getenv("PAYMENT_SERVICE_HOST");
    private static final String PAYMENT_PORT_ENV = System.getenv("PAYMENT_SERVICE_PORT");

    private static final String PAYMENT_SCHEMA = PAYMENT_SCHEMA_ENV == null ? "http" : PAYMENT_SCHEMA_ENV;
    private static final String PAYMENT_HOST = PAYMENT_HOST_ENV == null ? "localhost" : PAYMENT_HOST_ENV;

    /**
     * Liefert ein `ServiceValue`-Objekt für einen angegebenen Service.
     * @param service Name des Service (z.B. "invoice")
     * @return ServiceValue mit `schema`, `host` und `port`
     */
    public static ServiceValue getServiceValue(final String service) {
        return switch (service) {
            case "payment" -> {
                int port;
                int defaultPort = 7201;
                try {
                    port = PAYMENT_PORT_ENV == null ? defaultPort  : Integer.parseInt(PAYMENT_PORT_ENV);
                } catch (NumberFormatException e) {
                    LOGGER.warn("PAYMENT_SERVICE_PORT ist ungültig: '{}'. Fallback auf {}.", PAYMENT_PORT_ENV, defaultPort );
                    port = defaultPort ;
                }
                yield new ServiceValue(
                    PAYMENT_SCHEMA,
                    PAYMENT_HOST,
                    port
                );
            }
            default -> throw new IllegalStateException("Unbekannter Service: " + service);
        };
    }

    // Verhindert, dass diese Klasse instanziiert wird
    private Constants() {
        throw new UnsupportedOperationException("Diese Klasse darf nicht instanziiert werden.");
    }
}
