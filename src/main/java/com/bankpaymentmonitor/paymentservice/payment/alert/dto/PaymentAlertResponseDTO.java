package com.bankpaymentmonitor.paymentservice.payment.alert.dto;

import java.time.LocalDateTime;

public record PaymentAlertResponseDTO(
        Long id,
        String paymentReference,
        String branchCode,
        String type,
        String status,
        LocalDateTime detectedAt,
        LocalDateTime resolvedAt
) {
}