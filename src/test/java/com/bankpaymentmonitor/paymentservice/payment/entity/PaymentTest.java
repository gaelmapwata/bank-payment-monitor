package com.bankpaymentmonitor.paymentservice.payment.entity;

import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertService;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.exception.InvalidPaymentStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentTest {

    @Test
    void shouldMarkPendingPaymentAsProcessing() {
        LocalDateTime now =
                LocalDateTime.of(2026, 10, 1, 12, 0);

        Payment payment = new Payment(
                "PAY-001",
                "DEMO_SYSTEM",
                "TXN-001",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        assertEquals(PaymentStatus.PENDING, payment.getStatus());

        payment.markAsProcessing(now);

        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
    }
    @Test
    void shouldNotMarkPaymentAsProcessingWhenAlreadyProcessing() {

        LocalDateTime creationTime =
                LocalDateTime.of(2026, 10, 2, 8, 30);

        Payment payment = new Payment(
                "PAY-001",
                "DEMO_SYSTEM",
                "TXN-001",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                creationTime
        );

        LocalDateTime processingTime =
                LocalDateTime.of(2026, 10, 2, 8, 35);

        payment.markAsProcessing(processingTime);

        InvalidPaymentStatusTransitionException exception =
                assertThrows(
                        InvalidPaymentStatusTransitionException.class,
                        () -> payment.markAsProcessing(processingTime)
                );

        assertEquals(
                "Cannot change payment status from PROCESSING to PROCESSING",
                exception.getMessage()
        );

    }
    @Test
    void shouldMarkProcessingPaymentAsSuccess() {
        LocalDateTime now =
                LocalDateTime.of(2026, 10, 1, 12, 0);

        Payment payment = new Payment(
                "PAY-003",
                "DEMO_SYSTEM",
                "TXN-003",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        payment.markAsProcessing(now);

        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());

        payment.markAsSuccess(now);

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
    }
    @Test
    void shouldNotMarkPendingPaymentAsSuccess() {
        LocalDateTime now =
                LocalDateTime.of(2026, 10, 1, 12, 0);

        Payment payment = new Payment(
                "PAY-004",
                "DEMO_SYSTEM",
                "TXN-004",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );
        InvalidPaymentStatusTransitionException exception =
                assertThrows(
                        InvalidPaymentStatusTransitionException.class,
                        () -> payment.markAsSuccess(now)
                );

        assertEquals(
                "Cannot change payment status from PENDING to SUCCESS",
                exception.getMessage()
        );

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }
    @Test
    void shouldMarkProcessingPaymentAsFailed() {
        LocalDateTime now =
                LocalDateTime.of(2026, 10, 1, 12, 0);

        Payment payment = new Payment(
                "PAY-005",
                "DEMO_SYSTEM",
                "TXN-005",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        payment.markAsProcessing(now);

        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());

        payment.markAsFailed(now);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }
    @Test
    void shouldNotMarkPendingPaymentAsFailed() {
        LocalDateTime now =
                LocalDateTime.of(2026, 10, 1, 12, 0);

        Payment payment = new Payment(
                "PAY-006",
                "DEMO_SYSTEM",
                "TXN-006",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        InvalidPaymentStatusTransitionException exception =
                assertThrows(
                        InvalidPaymentStatusTransitionException.class,
                        () -> payment.markAsFailed(now)
                );

        assertEquals(
                "Cannot change payment status from PENDING to FAILED",
                exception.getMessage()
        );

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }
    @Test
    void shouldUseProvidedCreationTime() {

        LocalDateTime creationTime =
                LocalDateTime.of(2026, 10, 2, 8, 30);

        Payment payment = new Payment(
                "PAY-020",
                "DEMO_SYSTEM",
                "TXN-020",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                creationTime
        );

        assertEquals(creationTime, payment.getCreatedAt());
        assertEquals(creationTime, payment.getUpdatedAt());
    }

}