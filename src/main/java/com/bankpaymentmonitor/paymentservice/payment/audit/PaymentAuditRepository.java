package com.bankpaymentmonitor.paymentservice.payment.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAuditRepository
        extends JpaRepository<PaymentAudit, Long> {
    List<PaymentAudit> findByPaymentReferenceOrderByOccurredAtAsc(
            String paymentReference
    );
}