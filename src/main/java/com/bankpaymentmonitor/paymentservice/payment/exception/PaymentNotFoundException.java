package com.bankpaymentmonitor.paymentservice.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String reference) {
        super("Payment not found with reference: " + reference);
    }
}