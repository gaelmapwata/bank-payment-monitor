package com.bankpaymentmonitor.paymentservice.payment.audit.dto;

import com.bankpaymentmonitor.paymentservice.payment.audit.PaymentAuditAction;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentAuditResponseDTOTest {

    @Test
    void shouldCreatePaymentAuditResponseDTO() {

        // GIVEN
        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 10, 8, 11, 0);

        // WHEN
        PaymentAuditResponseDTO dto =
                new PaymentAuditResponseDTO(
                        15L,
                        "PAY-2026-001",
                        PaymentAuditAction.STATUS_CHANGE,
                        PaymentStatus.PENDING,
                        PaymentStatus.PROCESSING,
                        "john",
                        "BR-001",
                        occurredAt
                );

        // THEN
        assertEquals(15L, dto.id());
        assertEquals("PAY-2026-001", dto.paymentReference());
        assertEquals(PaymentAuditAction.STATUS_CHANGE, dto.action());
        assertEquals(PaymentStatus.PENDING, dto.previousStatus());
        assertEquals(PaymentStatus.PROCESSING, dto.newStatus());
        assertEquals("john", dto.performedBy());
        assertEquals("BR-001", dto.branchCode());
        assertEquals(occurredAt, dto.occurredAt());
    }
}