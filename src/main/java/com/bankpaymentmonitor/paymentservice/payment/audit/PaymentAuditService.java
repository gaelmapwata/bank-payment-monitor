package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class PaymentAuditService {

    private final PaymentAuditRepository paymentAuditRepository;
    private final Clock clock;

    public PaymentAuditService(
            PaymentAuditRepository paymentAuditRepository,
            Clock clock
    ) {
        this.paymentAuditRepository = paymentAuditRepository;
        this.clock = clock;
    }

    public void recordStatusChange(
            String paymentReference,
            PaymentStatus previousStatus,
            PaymentStatus newStatus
    ) {
        PaymentAudit audit = new PaymentAudit(
                paymentReference,
                PaymentAuditAction.STATUS_CHANGE,
                previousStatus,
                newStatus,
                LocalDateTime.now(clock)
        );

        paymentAuditRepository.save(audit);
    }
}