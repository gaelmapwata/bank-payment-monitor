package com.bankpaymentmonitor.paymentservice.payment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private PaymentOutboxPublisherRepository outboxRepository;

    @Test
    void shouldMarkEventAsPublishedOnlyAfterKafkaAcknowledgement() {
        UUID eventId = UUID.randomUUID();

        String paymentReference = "PAY-001";
        String payload = "{\"eventType\":\"PAYMENT_CREATED\"}";

        CompletableFuture<org.springframework.kafka.support.SendResult<String, String>>
                kafkaFuture = new CompletableFuture<>();

        when(kafkaTemplate.send(
                "bank.payment.events",
                paymentReference,
                payload
        )).thenReturn(kafkaFuture);

        PaymentOutboxPublisher publisher = new PaymentOutboxPublisher(
                kafkaTemplate,
                outboxRepository
        );

        publisher.publish(eventId, paymentReference, payload);

        // Kafka n'a pas encore confirmé : aucun marquage autorisé.
        verify(outboxRepository, never()).markAsPublished(any());

        // Kafka confirme la publication.
        kafkaFuture.complete(mock(org.springframework.kafka.support.SendResult.class));

        // Le marquage doit maintenant avoir lieu.
        verify(outboxRepository).markAsPublished(eventId);
    }
    @Test
    void shouldNotMarkEventAsPublishedWhenKafkaFails() {
        UUID eventId = UUID.randomUUID();

        String paymentReference = "PAY-002";
        String payload = "{\"eventType\":\"PAYMENT_CREATED\"}";

        CompletableFuture<SendResult<String, String>> kafkaFuture =
                new CompletableFuture<>();

        when(kafkaTemplate.send(
                "bank.payment.events",
                paymentReference,
                payload
        )).thenReturn(kafkaFuture);

        PaymentOutboxPublisher publisher = new PaymentOutboxPublisher(
                kafkaTemplate,
                outboxRepository
        );

        publisher.publish(eventId, paymentReference, payload);

        kafkaFuture.completeExceptionally(
                new RuntimeException("Kafka broker unavailable")
        );

        verify(outboxRepository, never()).markAsPublished(any());
    }
    @Test
    void shouldKeepEventUnpublishedWhenKafkaSendThrowsImmediately() {
        UUID eventId = UUID.randomUUID();

        String paymentReference = "PAY-003";
        String payload = "{\"eventType\":\"PAYMENT_CREATED\"}";

        when(kafkaTemplate.send(
                "bank.payment.events",
                paymentReference,
                payload
        )).thenThrow(new RuntimeException("Kafka producer unavailable"));

        PaymentOutboxPublisher publisher = new PaymentOutboxPublisher(
                kafkaTemplate,
                outboxRepository
        );

        assertDoesNotThrow(() ->
                publisher.publish(eventId, paymentReference, payload)
        );

        verify(outboxRepository, never()).markAsPublished(any());
    }
    @Test
    void shouldMarkEventAsPublishedWithCorrectWorkerId() {

        UUID eventId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        String paymentReference = "PAY-004";
        String payload = "{\"eventType\":\"PAYMENT_CREATED\"}";

        CompletableFuture<SendResult<String, String>> kafkaFuture =
                new CompletableFuture<>();

        when(kafkaTemplate.send(
                "bank.payment.events",
                paymentReference,
                payload
        )).thenReturn(kafkaFuture);

        PaymentOutboxPublisher publisher = new PaymentOutboxPublisher(
                kafkaTemplate,
                outboxRepository
        );

        // Act
        publisher.publish(
                eventId,
                paymentReference,
                payload,
                workerId
        );

        // Avant confirmation Kafka
        verify(outboxRepository, never())
                .markAsPublished(eventId, workerId);

        // Simuler une confirmation Kafka
        kafkaFuture.complete(null);

        // Après confirmation Kafka
        verify(outboxRepository, times(1))
                .markAsPublished(eventId, workerId);
    }
}