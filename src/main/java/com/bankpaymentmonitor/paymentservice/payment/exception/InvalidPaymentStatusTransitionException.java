package com.bankpaymentmonitor.paymentservice.payment.exception;

import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;

public class InvalidPaymentStatusTransitionException extends RuntimeException {

    public InvalidPaymentStatusTransitionException(
            PaymentStatus currentStatus,
            PaymentStatus targetStatus
    ) {
        super(
                "Cannot change payment status from "
                        + currentStatus
                        + " to "
                        + targetStatus
        );
    }
}