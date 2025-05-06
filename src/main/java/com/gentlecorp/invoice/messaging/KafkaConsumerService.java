package com.gentlecorp.invoice.messaging;

import com.gentlecorp.invoice.models.dto.NewPaymentIdDTO;
import com.gentlecorp.invoice.service.InvoiceWriteService;
import com.gentlecorp.invoice.tracing.LoggerPlus;
import com.gentlecorp.invoice.tracing.LoggerPlusFactory;
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

import static com.gentlecorp.invoice.messaging.KafkaTopicProperties.TOPIC_NEW_PAYMENT_ID;
import static com.gentlecorp.invoice.messaging.KafkaTopicProperties.TOPIC_SYSTEM_SHUTDOWN;

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


    @Observed(name = "kafka-consume.system-shutdown")
    @KafkaListener(topics = TOPIC_SYSTEM_SHUTDOWN, groupId = "${app.groupId}")
    public void consumeShutDown() {
        System.out.println("Shutting down via ApplicationContext");
        System.out.println("Bye 🖐🏾");
        ((ConfigurableApplicationContext) context).close();
    }
}
