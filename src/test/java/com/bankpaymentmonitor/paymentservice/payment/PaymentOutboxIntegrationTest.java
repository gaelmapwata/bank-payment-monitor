package com.bankpaymentmonitor.paymentservice.payment;

import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentCreateDTO;
import com.bankpaymentmonitor.paymentservice.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaymentOutboxIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateOutboxEventWhenPaymentIsCreated() {

        String sourceReference = UUID.randomUUID().toString();

        PaymentCreateDTO request = new PaymentCreateDTO(
                "TEST_SYSTEM",
                sourceReference,
                "BR-001",
                new BigDecimal("150.00"),
                "USD"
        );

        var payment = paymentService.createPayment(request);

        Integer eventCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM payment_outbox
                WHERE aggregate_id = ?
                  AND event_type = 'PAYMENT_CREATED'
                  AND published_at IS NULL
                """,
                Integer.class,
                payment.reference()
        );

        assertEquals(1, eventCount);
    }
    @Test
    void shouldRollbackPaymentWhenOutboxInsertionFails() {
        String sourceReference = UUID.randomUUID().toString();

        PaymentCreateDTO request = new PaymentCreateDTO(
                "TEST_SYSTEM",
                sourceReference,
                "BR-001",
                new BigDecimal("150.00"),
                "USD"
        );

        // Simuler une erreur lors de l'insertion Outbox
        jdbcTemplate.execute("""
            ALTER TABLE payment_outbox
            ADD CONSTRAINT chk_outbox_event_type
            CHECK (event_type <> 'PAYMENT_CREATED') NOT VALID
        """);

        try {
            assertThrows(
                    Exception.class,
                    () -> paymentService.createPayment(request)
            );

            Integer paymentCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM payments
                    WHERE source_system = ?
                      AND source_payment_reference = ?
                    """,
                    Integer.class,
                    "TEST_SYSTEM",
                    sourceReference
            );

            assertEquals(0, paymentCount);

        } finally {
            jdbcTemplate.execute("""
            ALTER TABLE payment_outbox
            DROP CONSTRAINT IF EXISTS chk_outbox_event_type
            """);
        }
    }
}