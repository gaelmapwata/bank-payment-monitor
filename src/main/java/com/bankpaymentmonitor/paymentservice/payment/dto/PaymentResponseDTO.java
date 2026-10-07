package com.bankpaymentmonitor.paymentservice.payment.dto;

import java.math.BigDecimal;

public record PaymentResponseDTO(
        String reference,
        String sourceSystem,
        String sourcePaymentReference,
        String branchCode,
        BigDecimal amount,
        String currency,
        String status
) {
}
