package com.bankpaymentmonitor.paymentservice.payment.entity;

import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.exception.InvalidPaymentStatusTransitionException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_reference",
                        columnNames = {"reference"}
                ),
                @UniqueConstraint(
                        name = "uk_payment_source_reference",
                        columnNames = {"source_system", "source_payment_reference"}
                )
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String reference;

    @Column(name = "source_system", nullable = false, length = 50)
    private String sourceSystem;

    @Column(name = "source_payment_reference", nullable = false, length = 100)
    private String sourcePaymentReference;

    @Column(name = "branch_code", nullable = false, length = 30)
    private String branchCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    protected Payment() {
        // Nécessaire à JPA / Hibernate
    }

    public Payment(  String reference,
                        String sourceSystem,
                        String sourcePaymentReference,
                        String branchCode,
                        BigDecimal amount,
                        String currency,
                      LocalDateTime now
    ) {
        this.reference = reference;
        this.sourceSystem = sourceSystem;
        this.sourcePaymentReference = sourcePaymentReference;
        this.branchCode = branchCode;
        this.amount = amount;
        this.currency = currency;

        this.status = PaymentStatus.PENDING;

        this.createdAt = now;
        this.updatedAt = now;
    }
    public void markAsProcessing(LocalDateTime now) {

        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStatusTransitionException(
                    this.status,
                    PaymentStatus.PROCESSING
            );
        }

        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = now;
    }
    public void markAsSuccess(LocalDateTime now) {

        if (this.status != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentStatusTransitionException(
                    this.status,
                    PaymentStatus.SUCCESS
            );
        }

        this.status = PaymentStatus.SUCCESS;
        this.updatedAt = now;
    }
    public void markAsFailed(LocalDateTime now) {

        if (this.status != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentStatusTransitionException(
                    this.status,
                    PaymentStatus.FAILED
            );
        }

        this.status = PaymentStatus.FAILED;
        this.updatedAt = now;
    }
}