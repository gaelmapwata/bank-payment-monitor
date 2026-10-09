package com.bankpaymentmonitor.paymentservice.payment.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Service
public class PaymentOutboxPublisher {

    private static final String TOPIC = "bank.payment.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentOutboxPublisherRepository outboxRepository;

    private static final Logger log =
            LoggerFactory.getLogger(PaymentOutboxPublisher.class);

    public PaymentOutboxPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            PaymentOutboxPublisherRepository outboxRepository
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepository = outboxRepository;
    }

    public void publish(UUID eventId, String paymentReference, String payload) {

        try {
            kafkaTemplate.send(
                    "bank.payment.events",
                    paymentReference,
                    payload
            ).whenComplete((result, exception) -> {

                if (exception == null) {
                    outboxRepository.markAsPublished(eventId);
                } else {
                    log.error(
                            "Kafka publication failed for eventId={}, paymentReference={}",
                            eventId,
                            paymentReference,
                            exception
                    );
                }
            });

        } catch (Exception exception) {
            log.error(
                    "Kafka send failed immediately for eventId={}, paymentReference={}",
                    eventId,
                    paymentReference,
                    exception
            );
        }
    }
    public void publish(
            UUID eventId,
            String paymentReference,
            String payload,
            UUID workerId
    ) {
        try {
            kafkaTemplate.send(
                    "bank.payment.events",
                    paymentReference,
                    payload
            ).whenComplete((result, exception) -> {

                if (exception == null) {
                    try {
                        outboxRepository.markAsPublished(eventId, workerId);

                        log.info(
                                "Outbox event confirmed: eventId={}, workerId={}",
                                eventId,
                                workerId
                        );

                    } catch (Exception dbException) {
                        log.error(
                                "Failed to confirm Outbox event in database: eventId={}, workerId={}",
                                eventId,
                                workerId,
                                dbException
                        );
                    }

                } else {
                    log.error(
                            "Kafka publication failed: eventId={}, workerId={}",
                            eventId,
                            workerId,
                            exception
                    );
                }
            });

        } catch (Exception exception) {
            log.error(
                    "Kafka send failed immediately: eventId={}, workerId={}",
                    eventId,
                    workerId,
                    exception
            );
        }
    }
}