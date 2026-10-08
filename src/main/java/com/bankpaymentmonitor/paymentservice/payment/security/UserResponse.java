package com.bankpaymentmonitor.paymentservice.payment.security;

public record UserResponse(
        String username,
        String branchCode,
        Role role
) {}