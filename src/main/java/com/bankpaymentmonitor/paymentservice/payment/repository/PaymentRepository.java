package com.bankpaymentmonitor.paymentservice.payment.repository;

import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReference(String reference);
    boolean existsBySourceSystemAndSourcePaymentReference(
            String sourceSystem,
            String sourcePaymentReference
    );
    List<Payment> findByBranchCode(String branchCode);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByStatusAndCreatedAtBefore(
            PaymentStatus status,
            LocalDateTime createdAt
    );
    List<Payment> findByStatusAndBranchCode(
            PaymentStatus status,
            String branchCode
    );
    List<Payment> findByStatusAndBranchCodeAndCreatedAtBefore(
            PaymentStatus status,
            String branchCode,
            LocalDateTime createdAt
    );
}