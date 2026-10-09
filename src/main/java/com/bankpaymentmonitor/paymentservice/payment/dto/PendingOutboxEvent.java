package com.bankpaymentmonitor.paymentservice.payment.dto;

import java.util.UUID;

public record PendingOutboxEvent(
        UUID id,
        String paymentReference,
        String payload
) {
}