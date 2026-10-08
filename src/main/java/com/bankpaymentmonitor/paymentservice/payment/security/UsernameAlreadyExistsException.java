package com.bankpaymentmonitor.paymentservice.payment.security;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}