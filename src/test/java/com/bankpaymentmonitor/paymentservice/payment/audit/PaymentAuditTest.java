package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentAuditTest {

    @Test
    void shouldCreateStatusChangeAudit() {

        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 10, 7, 10, 30);

        PaymentAudit audit = new PaymentAudit(
                "PAY-2026-001",
                PaymentAuditAction.STATUS_CHANGE,
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING,
                occurredAt
        );

        assertEquals(
                "PAY-2026-001",
                audit.getPaymentReference()
        );

        assertEquals(
                PaymentAuditAction.STATUS_CHANGE,
                audit.getAction()
        );

        assertEquals(
                PaymentStatus.PENDING,
                audit.getPreviousStatus()
        );

        assertEquals(
                PaymentStatus.PROCESSING,
                audit.getNewStatus()
        );

        assertEquals(
                occurredAt,
                audit.getOccurredAt()
        );
    }
    @Test
    void shouldRejectAuditWithoutPaymentReference() {

        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 10, 7, 10, 30);

        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentAudit(
                        "   ",
                        PaymentAuditAction.STATUS_CHANGE,
                        PaymentStatus.PENDING,
                        PaymentStatus.PROCESSING,
                        occurredAt
                )
        );
    }
    @Test
    void shouldRejectAuditWithoutAction() {

        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 10, 7, 10, 30);

        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentAudit(
                        "PAY-2026-001",
                        null,
                        PaymentStatus.PENDING,
                        PaymentStatus.PROCESSING,
                        occurredAt
                )
        );
    }
    @Test
    void shouldRejectStatusChangeAuditWithoutPreviousStatus() {

        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 10, 7, 10, 30);

        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentAudit(
                        "PAY-2026-001",
                        PaymentAuditAction.STATUS_CHANGE,
                        null,
                        PaymentStatus.PROCESSING,
                        occurredAt
                )
        );
    }
    @Test
    void shouldRejectStatusChangeAuditWithoutNewStatus() {

        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 10, 7, 10, 30);

        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentAudit(
                        "PAY-2026-001",
                        PaymentAuditAction.STATUS_CHANGE,
                        PaymentStatus.PENDING,
                        null,
                        occurredAt
                )
        );
    }
    @Test
    void shouldRejectStatusChangeWhenStatusDoesNotChange() {

        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 10, 7, 10, 30);

        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentAudit(
                        "PAY-2026-001",
                        PaymentAuditAction.STATUS_CHANGE,
                        PaymentStatus.PENDING,
                        PaymentStatus.PENDING,
                        occurredAt
                )
        );
    }
    @Test
    void shouldRejectAuditWithoutOccurredAt() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentAudit(
                        "PAY-2026-001",
                        PaymentAuditAction.STATUS_CHANGE,
                        PaymentStatus.PENDING,
                        PaymentStatus.PROCESSING,
                        null
                )
        );
    }
    @Test
    void shouldHaveNoIdBeforePersistence() {

        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 10, 7, 10, 30);

        PaymentAudit audit = new PaymentAudit(
                "PAY-2026-001",
                PaymentAuditAction.STATUS_CHANGE,
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING,
                occurredAt
        );

        assertNull(audit.getId());
    }
}