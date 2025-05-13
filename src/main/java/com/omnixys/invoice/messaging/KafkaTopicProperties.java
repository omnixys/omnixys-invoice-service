package com.omnixys.invoice.messaging;

import lombok.RequiredArgsConstructor;

/**
 * Zentrale Konfiguration der Kafka-Topic-Namen.
 * <p>
 * Die Namen folgen dem Schema: {@code <service>.<entities>.<events>}.
 * </p>
 *
 * @author Caleb
 * @since 20.04.2025
 */
@RequiredArgsConstructor
public final class KafkaTopicProperties {

    public static final String TOPIC_INVOICE_CREATE_PAYMENT = "invoice.create.payment";
    /** ✉️ Mailversand bei Kundenregistrierung */
    public static final String TOPIC_NOTIFICATION_ACCOUNT_CREATED = "notification.invoice.created";

    public static final String TOPIC_LOG_STREAM_LOG_INVOICE = "log-Stream.log.invoice";

    public static final String TOPIC_INVOICE_SHUTDOWN_ORCHESTRATOR = "invoice.shutdown.orchestrator";
    public static final String TOPIC_INVOICE_START_ORCHESTRATOR = "invoice.start.orchestrator";
    public static final String TOPIC_INVOICE_RESTART_ORCHESTRATOR = "invoice.restart.orchestrator";

    public static final String TOPIC_ALL_SHUTDOWN_ORCHESTRATOR = "all.shutdown.orchestrator";
    public static final String TOPIC_ALL_START_ORCHESTRATOR = "all.start.orchestrator";
    public static final String TOPIC_ALL_RESTART_ORCHESTRATOR = "all.restart.orchestrator";
}
