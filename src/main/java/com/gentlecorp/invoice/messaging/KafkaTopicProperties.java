package com.gentlecorp.invoice.messaging;

/**
 * Zentrale Konfiguration der Kafka-Topic-Namen.
 * <p>
 * Die Namen folgen dem Schema: {@code <service>.<entities>.<events>}.
 * </p>
 *
 * @author Caleb
 * @since 20.04.2025
 */
public final class KafkaTopicProperties {

    private KafkaTopicProperties() {
        // Utility class – private Konstruktor verhindert Instanziierung
    }

    public static final String TOPIC_NEW_PAYMENT_ID = "newPaymentId";
    /** ✉️ Mailversand bei Kundenregistrierung */
    public static final String TOPIC_NOTIFICATION_ACCOUNT_CREATED = "notification.invoice.created";

    public static final String TOPIC_ACTIVITY_EVENTS = "activity.invoice.log";

    public static final String TOPIC_SYSTEM_SHUTDOWN = "system.shutdown";
}
