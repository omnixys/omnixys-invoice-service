package com.omnixys.invoice;

import com.omnixys.invoice.config.AppProperties;
import com.omnixys.invoice.config.ApplicationConfig;
import com.omnixys.invoice.dev.DevConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.graphql.data.federation.FederationSchemaFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import static com.omnixys.invoice.util.Banner.TEXT;

@SpringBootApplication(proxyBeanMethods = false)
@Import({ApplicationConfig.class, DevConfig.class})
@EnableConfigurationProperties({AppProperties.class})
@EnableJpaRepositories
@EnableWebSecurity
@EnableMethodSecurity
@EnableAsync
@SuppressWarnings({"ClassUnconnectedToPackage"})
public class InvoiceApplication {

    public static void main(String[] args) {
        new Env();
  	    final var app = new SpringApplication(InvoiceApplication.class);
		app.setBanner((_, _, out) -> out.println(TEXT));
		app.run(args);
    }

    @Bean
    public GraphQlSourceBuilderCustomizer customizer(FederationSchemaFactory factory) {
        return builder -> builder.schemaFactory(factory::createGraphQLSchema);
    }

    @Bean
    FederationSchemaFactory federationSchemaFactory() {
        return new FederationSchemaFactory();
    }

}
