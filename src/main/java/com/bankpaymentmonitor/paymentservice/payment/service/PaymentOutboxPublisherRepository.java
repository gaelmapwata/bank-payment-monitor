package com.bankpaymentmonitor.paymentservice.payment.service;

import com.bankpaymentmonitor.paymentservice.payment.dto.PendingOutboxEvent;

import java.util.List;
import java.util.UUID;

public interface PaymentOutboxPublisherRepository {

    void markAsPublished(UUID eventId);
    List<PendingOutboxEvent> findPendingEvents(int limit);
    List<PendingOutboxEvent> claimPendingEvents(
            int limit,
            UUID workerId
    );
    void markAsPublished(UUID eventId, UUID workerId);
}