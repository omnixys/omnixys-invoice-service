package com.gentlecorp.invoice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import static com.gentlecorp.invoice.util.Constants.GRAPHQL_ENDPOINT;
import static com.gentlecorp.invoice.util.Constants.getServiceValue;


@SuppressWarnings("java:S1075")
sealed interface PaymentClientConfig permits ApplicationConfig {
    /**
     * Logger-Objekt für AccountClientConfig.
     */
    Logger LOGGER = LoggerFactory.getLogger(PaymentClientConfig.class);

    /**
     * Bean-Methode, um ein Objekt von UriComponentsBuilder für die URI für Keycloak zu erstellen.
     *
     * @return Objekt von UriComponentsBuilder für die URI für Keycloak
     */
    @Bean
    @SuppressWarnings("CallToSystemGetenv")
    default UriComponentsBuilder uriComponentsBuilder() {
        final var values = getServiceValue("payment");
        LOGGER.debug("payment: host={}, port={}", values.host(), values.port());
        return UriComponentsBuilder.newInstance()
            .scheme(values.schema())
            .host(values.host())
            .port(values.port());
    }

    @Bean
    default HttpGraphQlClient graphQlClient() {
        final var values = getServiceValue("payment");
        String graphQlUrl = UriComponentsBuilder.newInstance()
            .scheme(values.schema())
            .host(values.host())
            .port(values.port())
            .path(GRAPHQL_ENDPOINT)
            .toUriString();


        WebClient webClient = WebClient.builder()
            .baseUrl(graphQlUrl)
            .defaultHeader("Content-Type", "application/json")
            .build();
        return HttpGraphQlClient.builder(webClient).build();
    }

}
