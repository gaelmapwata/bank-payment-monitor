package com.bankpaymentmonitor.paymentservice.payment.exception;

public class PaymentAlreadyExistsException extends RuntimeException {

    public PaymentAlreadyExistsException(
            String sourceSystem,
            String sourcePaymentReference
    ) {
        super(
                "Payment already exists for source system: "
                        + sourceSystem
                        + " and source reference: "
                        + sourcePaymentReference
        );
    }
}