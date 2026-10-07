package com.bankpaymentmonitor.paymentservice.payment.alert;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentAlertTest {

    @Test
    void shouldCreateAlertAsOpen() {

        LocalDateTime detectedAt =
                LocalDateTime.of(2026, 10, 2, 12, 0);

        PaymentAlert alert = new PaymentAlert(
                "PAY-TEST-001",
                "BR-001",
                PaymentAlertType.STALE_PENDING,
                detectedAt
        );

        assertEquals(
                PaymentAlertStatus.OPEN,
                alert.getStatus()
        );

        assertEquals(
                detectedAt,
                alert.getDetectedAt()
        );

        assertNull(alert.getResolvedAt());
    }

    @Test
    void shouldResolveOpenAlert() {

        PaymentAlert alert = new PaymentAlert(
                "PAY-TEST-002",
                "BR-001",
                PaymentAlertType.STALE_PENDING,
                LocalDateTime.of(2026, 10, 2, 12, 0)
        );

        LocalDateTime resolvedAt =
                LocalDateTime.of(2026, 10, 2, 12, 10);

        alert.resolve(resolvedAt);

        assertEquals(
                PaymentAlertStatus.RESOLVED,
                alert.getStatus()
        );

        assertEquals(
                resolvedAt,
                alert.getResolvedAt()
        );
    }

    @Test
    void shouldKeepOriginalResolutionWhenAlreadyResolved() {

        PaymentAlert alert = new PaymentAlert(
                "PAY-TEST-003",
                "BR-001",
                PaymentAlertType.STALE_PENDING,
                LocalDateTime.of(2026, 10, 2, 12, 0)
        );

        LocalDateTime firstResolution =
                LocalDateTime.of(2026, 10, 2, 12, 10);

        LocalDateTime secondResolution =
                LocalDateTime.of(2026, 10, 2, 12, 20);

        alert.resolve(firstResolution);
        alert.resolve(secondResolution);

        assertEquals(
                PaymentAlertStatus.RESOLVED,
                alert.getStatus()
        );

        assertEquals(
                firstResolution,
                alert.getResolvedAt()
        );
    }
}