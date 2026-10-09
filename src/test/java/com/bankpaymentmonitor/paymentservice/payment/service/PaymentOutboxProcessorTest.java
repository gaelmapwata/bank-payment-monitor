
package com.bankpaymentmonitor.paymentservice.payment.service;

import com.bankpaymentmonitor.paymentservice.payment.dto.PendingOutboxEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxProcessorTest {

    @Mock
    private PaymentOutboxPublisherRepository repository;

    @Mock
    private PaymentOutboxPublisher publisher;

    @InjectMocks
    private PaymentOutboxProcessor processor;

    @Test
    void shouldPublishPendingOutboxEvents() {

        UUID eventId = UUID.randomUUID();

        PendingOutboxEvent event = new PendingOutboxEvent(
                eventId,
                "PAY-001",
                "{\"eventType\":\"PAYMENT_CREATED\"}"
        );

        when(repository.claimPendingEvents(
                eq(100),
                any(UUID.class)
        )).thenReturn(List.of(event));

        processor.processPendingEvents();

        verify(publisher).publish(
                eq(eventId),
                eq("PAY-001"),
                eq(event.payload()),
                any(UUID.class)
        );
    }

    @Test
    void shouldNotPublishWhenNoPendingEvents() {

        when(repository.claimPendingEvents(
                eq(100),
                any(UUID.class)
        )).thenReturn(List.of());

        processor.processPendingEvents();

        verifyNoInteractions(publisher);

        verify(repository, times(1)).claimPendingEvents(
                eq(100),
                any(UUID.class)
        );
    }

    @Test
    void shouldPublishMultiplePendingOutboxEvents() {

        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        UUID eventId3 = UUID.randomUUID();

        PendingOutboxEvent event1 = new PendingOutboxEvent(
                eventId1,
                "PAY-001",
                "{\"eventType\":\"PAYMENT_CREATED\"}"
        );

        PendingOutboxEvent event2 = new PendingOutboxEvent(
                eventId2,
                "PAY-002",
                "{\"eventType\":\"PAYMENT_CREATED\"}"
        );

        PendingOutboxEvent event3 = new PendingOutboxEvent(
                eventId3,
                "PAY-003",
                "{\"eventType\":\"PAYMENT_CREATED\"}"
        );

        when(repository.claimPendingEvents(
                eq(100),
                any(UUID.class)
        )).thenReturn(List.of(event1, event2, event3));

        processor.processPendingEvents();

        InOrder inOrder = inOrder(publisher);

        ArgumentCaptor<UUID> workerIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        inOrder.verify(publisher).publish(
                eq(eventId1),
                eq("PAY-001"),
                eq(event1.payload()),
                workerIdCaptor.capture()
        );

        inOrder.verify(publisher).publish(
                eq(eventId2),
                eq("PAY-002"),
                eq(event2.payload()),
                workerIdCaptor.capture()
        );

        inOrder.verify(publisher).publish(
                eq(eventId3),
                eq("PAY-003"),
                eq(event3.payload()),
                workerIdCaptor.capture()
        );

        // Tous les événements doivent utiliser le même workerId
        List<UUID> workerIds = workerIdCaptor.getAllValues();

        org.junit.jupiter.api.Assertions.assertEquals(
                workerIds.get(0),
                workerIds.get(1)
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                workerIds.get(1),
                workerIds.get(2)
        );

        verifyNoMoreInteractions(publisher);
    }

    @Test
    void shouldClaimPendingEventsBeforePublishing() {

        UUID eventId = UUID.randomUUID();

        PendingOutboxEvent event = new PendingOutboxEvent(
                eventId,
                "PAY-005",
                "{\"eventType\":\"PAYMENT_CREATED\"}"
        );

        when(repository.claimPendingEvents(
                eq(100),
                any(UUID.class)
        )).thenReturn(List.of(event));

        processor.processPendingEvents();

        ArgumentCaptor<UUID> workerIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        verify(repository).claimPendingEvents(
                eq(100),
                workerIdCaptor.capture()
        );

        UUID workerId = workerIdCaptor.getValue();

        verify(publisher).publish(
                eventId,
                "PAY-005",
                event.payload(),
                workerId
        );

        verify(repository, never()).findPendingEvents(anyInt());
    }
}
