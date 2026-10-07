package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payment_audits")
public class PaymentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_reference", nullable = false)
    private  String paymentReference;

    @Enumerated(EnumType.STRING)
    private PaymentAuditAction action;

    @Enumerated(EnumType.STRING)
    private PaymentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus newStatus;

    @Column(name = "occurred_at", nullable = false)
    private  LocalDateTime occurredAt;


    public PaymentAudit(
            String paymentReference,
            PaymentAuditAction action,
            PaymentStatus previousStatus,
            PaymentStatus newStatus,
            LocalDateTime occurredAt
    ) {
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment reference is required"
            );
        }
        if (action == null) {
            throw new IllegalArgumentException(
                    "Audit action is required"
            );
        }
        if (action == PaymentAuditAction.STATUS_CHANGE
                && previousStatus == null) {
            throw new IllegalArgumentException(
                    "Previous status is required for a status change"
            );
        }
        if (action == PaymentAuditAction.STATUS_CHANGE
                && newStatus == null) {
            throw new IllegalArgumentException(
                    "New status is required for a status change"
            );
        }
        if (action == PaymentAuditAction.STATUS_CHANGE
                && previousStatus == newStatus) {
            throw new IllegalArgumentException(
                    "Previous status and new status must be different"
            );
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException(
                    "Occurred at is required"
            );
        }
        this.paymentReference = paymentReference;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.occurredAt = occurredAt;
    }
    protected PaymentAudit() {
    }
}