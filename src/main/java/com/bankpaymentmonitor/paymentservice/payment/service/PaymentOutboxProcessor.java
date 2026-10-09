package com.bankpaymentmonitor.paymentservice.payment.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentOutboxProcessor {

    private final PaymentOutboxPublisherRepository repository;
    private final PaymentOutboxPublisher publisher;

    public PaymentOutboxProcessor(
            PaymentOutboxPublisherRepository repository,
            PaymentOutboxPublisher publisher
    ) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public void processPendingEvents() {

        UUID workerId = UUID.randomUUID();

        var pendingEvents = repository.claimPendingEvents(
                100,
                workerId
        );

        for (var event : pendingEvents) {
            publisher.publish(
                    event.id(),
                    event.paymentReference(),
                    event.payload(),
                    workerId
            );
        }
    }

}