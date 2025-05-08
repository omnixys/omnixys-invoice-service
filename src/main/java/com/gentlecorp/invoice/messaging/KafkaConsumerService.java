package com.omnixys.invoice.messaging;

import com.omnixys.invoice.models.dto.NewPaymentIdDTO;
import com.omnixys.invoice.service.InvoiceWriteService;
import com.omnixys.invoice.tracing.LoggerPlus;
import com.omnixys.invoice.tracing.LoggerPlusFactory;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

import static com.omnixys.invoice.messaging.KafkaTopicProperties.TOPIC_ALL_RESTART_ORCHESTRATOR;
import static com.omnixys.invoice.messaging.KafkaTopicProperties.TOPIC_ALL_SHUTDOWN_ORCHESTRATOR;
import static com.omnixys.invoice.messaging.KafkaTopicProperties.TOPIC_ALL_START_ORCHESTRATOR;
import static com.omnixys.invoice.messaging.KafkaTopicProperties.TOPIC_INVOICE_RESTART_ORCHESTRATOR;
import static com.omnixys.invoice.messaging.KafkaTopicProperties.TOPIC_INVOICE_SHUTDOWN_ORCHESTRATOR;
import static com.omnixys.invoice.messaging.KafkaTopicProperties.TOPIC_INVOICE_START_ORCHESTRATOR;
import static com.omnixys.invoice.messaging.KafkaTopicProperties.TOPIC_NEW_PAYMENT_ID;
import static com.omnixys.invoice.messaging.KafkaTopicProperties.TOPIC_SYSTEM_SHUTDOWN;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ApplicationContext context;
    private final InvoiceWriteService invoiceWriteService;
    private final Tracer tracer;
    private final LoggerPlusFactory factory;
    private LoggerPlus logger() {
        return factory.getLogger(getClass());
    }

    @KafkaListener(topics = TOPIC_NEW_PAYMENT_ID, groupId = "${app.groupId}")
    @Observed(name = "invoice-service.write.finalize-payment")
    public void consumeFinalizePayment(ConsumerRecord<String, NewPaymentIdDTO> record) {
        final var headers = record.headers();
        final var newPaymentIdDTO = record.value();

        // ✨ 1. Extrahiere traceparent Header (W3C) oder B3 als Fallback
        final var traceParent = getHeader(headers, "traceparent");

        SpanContext linkedContext = null;
        if (traceParent != null && traceParent.startsWith("00-")) {
            String[] parts = traceParent.split("-");
            if (parts.length == 4) {
                String traceId = parts[1];
                String spanId = parts[2];
                boolean sampled = "01".equals(parts[3]);

                linkedContext = SpanContext.createFromRemoteParent(
                    traceId,
                    spanId,
                    sampled ? TraceFlags.getSampled() : TraceFlags.getDefault(),
                    TraceState.getDefault()
                );
            }
        }

        // ✨ 2. Starte neuen Trace mit Link (nicht als Parent!)
        SpanBuilder spanBuilder = tracer.spanBuilder("kafka.invoice.consume")
            .setSpanKind(SpanKind.CONSUMER)
            .setAttribute("messaging.system", "kafka")
            .setAttribute("messaging.destination", TOPIC_NEW_PAYMENT_ID)
            .setAttribute("messaging.operation", "consume");

        if (linkedContext != null && linkedContext.isValid()) {
            spanBuilder.addLink(linkedContext);
        }

        Span span = spanBuilder.startSpan();

        try (Scope scope = span.makeCurrent()) {
            assert scope != null;
            logger().info("📥 Empfangene Nachricht auf '{}': {}", TOPIC_NEW_PAYMENT_ID, newPaymentIdDTO);
            invoiceWriteService.finalizePayment(newPaymentIdDTO);
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Kafka-Fehler");
            logger().error("❌ Fehler beim Erstellen des Kontos", e);
        } finally {
            span.end();
        }
    }

    private String getHeader(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }

    @Observed(name = "kafka-consume.invoice.orchestration")
    @KafkaListener(
        topics = {
            TOPIC_INVOICE_SHUTDOWN_ORCHESTRATOR,
            TOPIC_INVOICE_START_ORCHESTRATOR,
            TOPIC_INVOICE_RESTART_ORCHESTRATOR
        },
        groupId = "${app.groupId}"
    )
    public void handlePersonScoped(ConsumerRecord<String, String> record) {
        final String topic = record.topic();
        logger().info("Person-spezifisches Kommando empfangen: {}", topic);

        switch (topic) {
            case TOPIC_INVOICE_SHUTDOWN_ORCHESTRATOR -> shutdown();
            case TOPIC_INVOICE_RESTART_ORCHESTRATOR -> restart();
            case TOPIC_INVOICE_START_ORCHESTRATOR -> logger().info("Startsignal für Person-Service empfangen");
        }
    }

    @Observed(name = "kafka-consume.all.orchestration")
    @KafkaListener(
        topics = {
            TOPIC_ALL_SHUTDOWN_ORCHESTRATOR,
            TOPIC_ALL_START_ORCHESTRATOR,
            TOPIC_ALL_RESTART_ORCHESTRATOR
        },
        groupId = "${app.groupId}"
    )
    public void handleGlobalScoped(ConsumerRecord<String, String> record) {
        final String topic = record.topic();
        logger().info("Globales Systemkommando empfangen: {}", topic);

        switch (topic) {
            case TOPIC_ALL_SHUTDOWN_ORCHESTRATOR -> shutdown();
            case TOPIC_ALL_RESTART_ORCHESTRATOR -> restart();
            case TOPIC_ALL_START_ORCHESTRATOR -> logger().info("Globales Startsignal empfangen");
        }
    }

    private void shutdown() {
        try {
            logger().info("→ Anwendung wird heruntergefahren (Shutdown-Kommando).");
            ((ConfigurableApplicationContext) context).close();
        } catch (Exception e) {
            logger().error("Fehler beim Shutdown: {}", e.getMessage(), e);
        }
    }


    private void restart() {
        logger().info("→ Anwendung wird neugestartet (Restart-Kommando).");
        ((ConfigurableApplicationContext) context).close();
        // Neustart durch externen Supervisor erwartet
    }
}
