package com.bankpaymentmonitor.paymentservice.payment.repository;

import com.bankpaymentmonitor.paymentservice.payment.dto.PendingOutboxEvent;
import com.bankpaymentmonitor.paymentservice.payment.service.PaymentOutboxPublisherRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcPaymentOutboxPublisherRepository
        implements PaymentOutboxPublisherRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPaymentOutboxPublisherRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void markAsPublished(UUID eventId) {
        jdbcTemplate.update(
                """
                UPDATE payment_outbox
                SET published_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND published_at IS NULL
                """,
                eventId
        );
    }
    @Override
    public List<PendingOutboxEvent> findPendingEvents(int limit) {

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }

        String sql = """
            SELECT
                id,
                aggregate_id,
                payload::text AS payload
            FROM payment_outbox
            WHERE published_at IS NULL
            ORDER BY created_at ASC, id ASC
            LIMIT ?
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new PendingOutboxEvent(
                        rs.getObject("id", UUID.class),
                        rs.getString("aggregate_id"),
                        rs.getString("payload")
                ),
                limit
        );
    }
    @Override
    public List<PendingOutboxEvent> claimPendingEvents(
            int limit,
            UUID workerId
    ) {
        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "Limit must be greater than zero"
            );
        }

        Objects.requireNonNull(workerId, "workerId is required");

        String sql = """
            WITH selected AS (
                SELECT id
                FROM payment_outbox
                WHERE published_at IS NULL
                  AND (
                      claimed_until IS NULL
                      OR claimed_until < CURRENT_TIMESTAMP
                  )
                ORDER BY created_at ASC, id ASC
                FOR UPDATE SKIP LOCKED
                LIMIT ?
            )
            UPDATE payment_outbox AS o
            SET claimed_by = ?,
                claimed_until = CURRENT_TIMESTAMP + INTERVAL '5 minutes'
            FROM selected
            WHERE o.id = selected.id
            RETURNING
                o.id,
                o.aggregate_id,
                o.payload::text AS payload
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new PendingOutboxEvent(
                        rs.getObject("id", UUID.class),
                        rs.getString("aggregate_id"),
                        rs.getString("payload")
                ),
                limit,
                workerId
        );
    }
    @Override
    public void markAsPublished(UUID eventId, UUID workerId) {

        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(workerId, "workerId is required");

        String sql = """
            UPDATE payment_outbox
            SET published_at = CURRENT_TIMESTAMP,
                claimed_by = NULL,
                claimed_until = NULL
            WHERE id = ?
              AND claimed_by = ?
              AND claimed_until > CURRENT_TIMESTAMP
              AND published_at IS NULL
            """;

        jdbcTemplate.update(sql, eventId, workerId);
    }
}