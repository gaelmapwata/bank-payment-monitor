package com.bankpaymentmonitor.paymentservice.payment.audit.dto;

import com.bankpaymentmonitor.paymentservice.payment.audit.PaymentAuditAction;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentAuditResponseDTO(
        Long id,
        String paymentReference,
        PaymentAuditAction action,
        PaymentStatus previousStatus,
        PaymentStatus newStatus,
        String performedBy,
        String branchCode,
        LocalDateTime occurredAt
) {
}