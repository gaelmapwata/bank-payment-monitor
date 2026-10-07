package com.bankpaymentmonitor.paymentservice.payment.security;

import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class BranchAccessService {

    private final PaymentRepository paymentRepository;

    public BranchAccessService(
             PaymentRepository paymentRepository
    ) {
        this.paymentRepository = paymentRepository;
    }

    public boolean canAccessBranch(
            CustomUserPrincipal principal,
            String requestedBranchCode
    ) {
        if (principal.getRole() == Role.ADMIN) {
            return true;
        }

        return principal.getBranchCode()
                .equals(requestedBranchCode);
    }
    public boolean canAccessPaymentAlerts(
            CustomUserPrincipal principal,
            String paymentReference
    ) {
        return canAccessPayment(principal, paymentReference);
    }
    public boolean canAccessPayment(
            CustomUserPrincipal principal,
            String paymentReference
    ) {

        if (principal.getRole() == Role.ADMIN) {
            return true;
        }

        return paymentRepository
                .findByReference(paymentReference)
                .map(payment ->
                        principal.getBranchCode()
                                .equals(payment.getBranchCode())
                )
                .orElse(false);
    }
}