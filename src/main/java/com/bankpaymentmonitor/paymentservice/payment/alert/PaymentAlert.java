package com.bankpaymentmonitor.paymentservice.payment.alert;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "payment_alerts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_alert",
                        columnNames = {"payment_reference", "alert_type"}
                )
        }
)
    public class PaymentAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_reference", nullable = false, length = 100)
    private String paymentReference;

    @Column(name = "branch_code", nullable = false, length = 30)
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private PaymentAlertType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentAlertStatus status;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    protected PaymentAlert() {
    }

    public PaymentAlert(
            String paymentReference,
            String branchCode,
            PaymentAlertType type,
            LocalDateTime detectedAt
    ) {
        this.paymentReference = paymentReference;
        this.branchCode = branchCode;
        this.type = type;
        this.status = PaymentAlertStatus.OPEN;
        this.detectedAt = detectedAt;
    }

    public void resolve(LocalDateTime resolvedAt) {

        if (this.status == PaymentAlertStatus.RESOLVED) {
            return;
        }

        this.status = PaymentAlertStatus.RESOLVED;
        this.resolvedAt = resolvedAt;
    }
}