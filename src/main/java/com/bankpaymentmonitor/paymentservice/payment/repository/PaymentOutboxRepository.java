package com.bankpaymentmonitor.paymentservice.payment.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Repository
public class PaymentOutboxRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PaymentOutboxRepository(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void savePaymentCreated(
            String paymentReference,
            String sourceSystem
    ) {
        UUID eventId = UUID.randomUUID();

        String payload = objectMapper.writeValueAsString(
                Map.of(
                        "eventId", eventId.toString(),
                        "paymentReference", paymentReference,
                        "eventType", "PAYMENT_CREATED",
                        "sourceSystem", sourceSystem
                )
        );

        jdbcTemplate.update(
                """
                INSERT INTO payment_outbox
                    (id, aggregate_id, event_type, payload, created_at)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?)
                """,
                eventId,
                paymentReference,
                "PAYMENT_CREATED",
                payload,
                LocalDateTime.now(clock)
        );
    }
}