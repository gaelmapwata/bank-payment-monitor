package com.bankpaymentmonitor.paymentservice.exception;

public record ApiErrorResponse(
        int status,
        String error,
        String message
) {
}