package com.bankpaymentmonitor.paymentservice.payment.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentAlertRepository
        extends JpaRepository<PaymentAlert, Long> {

    boolean existsByPaymentReferenceAndType(
            String paymentReference,
            PaymentAlertType type
    );
    List<PaymentAlert> findByPaymentReference(String paymentReference);
    List<PaymentAlert> findByPaymentReferenceAndStatus(
            String paymentReference,
            PaymentAlertStatus status
    );
    List<PaymentAlert> findByBranchCode(
            String branchCode
    );
    List<PaymentAlert> findByBranchCodeAndStatus(
            String branchCode,
            PaymentAlertStatus status
    );
    Optional<PaymentAlert> findFirstByPaymentReference(String paymentReference);
}