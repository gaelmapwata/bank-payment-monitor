package com.bankpaymentmonitor.paymentservice.payment.alert.mapper;

import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlert;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertStatus;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertType;
import com.bankpaymentmonitor.paymentservice.payment.alert.dto.PaymentAlertResponseDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentAlertMapperTest {

    private final PaymentAlertMapper mapper =
            new PaymentAlertMapper();

    @Test
    void shouldMapOpenAlertToResponseDTO() {

        LocalDateTime detectedAt =
                LocalDateTime.of(2026, 10, 2, 10, 0);

        PaymentAlert alert = new PaymentAlert(
                "PAY-001",
                "BR-001",
                PaymentAlertType.STALE_PENDING,
                detectedAt
        );

        PaymentAlertResponseDTO response =
                mapper.toResponseDTO(alert);

        assertEquals(
                "PAY-001",
                response.paymentReference()
        );

        assertEquals(
                "BR-001",
                response.branchCode()
        );

        assertEquals(
                "STALE_PENDING",
                response.type()
        );

        assertEquals(
                "OPEN",
                response.status()
        );

        assertEquals(
                detectedAt,
                response.detectedAt()
        );

        assertNull(
                response.resolvedAt()
        );
    }
    @Test
    void shouldMapResolvedAlertToResponseDTO() {

        LocalDateTime detectedAt =
                LocalDateTime.of(2026, 10, 2, 10, 0);

        LocalDateTime resolvedAt =
                LocalDateTime.of(2026, 10, 2, 10, 15);

        PaymentAlert alert = new PaymentAlert(
                "PAY-002",
                "BR-001",
                PaymentAlertType.STALE_PENDING,
                detectedAt
        );

        alert.resolve(resolvedAt);

        PaymentAlertResponseDTO response =
                mapper.toResponseDTO(alert);

        assertEquals(
                "PAY-002",
                response.paymentReference()
        );

        assertEquals(
                "BR-001",
                response.branchCode()
        );

        assertEquals(
                "STALE_PENDING",
                response.type()
        );

        assertEquals(
                "RESOLVED",
                response.status()
        );

        assertEquals(
                detectedAt,
                response.detectedAt()
        );

        assertEquals(
                resolvedAt,
                response.resolvedAt()
        );
    }
}