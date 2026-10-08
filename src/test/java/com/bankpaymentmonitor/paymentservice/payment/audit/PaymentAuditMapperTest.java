package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.audit.dto.PaymentAuditResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PaymentAuditMapperTest {

    @Test
    void shouldMapPaymentAuditToResponseDTO() {

        // GIVEN
        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 10, 8, 11, 0);

        PaymentAudit audit = new PaymentAudit(
                "PAY-2026-001",
                PaymentAuditAction.STATUS_CHANGE,
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING,
                occurredAt,
                "john",
                "BR-001"
        );

        // WHEN
        PaymentAuditResponseDTO dto =
                PaymentAuditMapper.toResponseDTO(audit);

        // THEN
        assertEquals("PAY-2026-001", dto.paymentReference());

        assertEquals(
                PaymentAuditAction.STATUS_CHANGE,
                dto.action()
        );

        assertEquals(
                PaymentStatus.PENDING,
                dto.previousStatus()
        );

        assertEquals(
                PaymentStatus.PROCESSING,
                dto.newStatus()
        );

        assertEquals("john", dto.performedBy());
        assertEquals("BR-001", dto.branchCode());
        assertEquals(occurredAt, dto.occurredAt());
    }
}