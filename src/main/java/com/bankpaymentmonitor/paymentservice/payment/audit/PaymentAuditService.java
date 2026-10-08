package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.security.CurrentUserProvider;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class PaymentAuditService {

    private final PaymentAuditRepository paymentAuditRepository;
    private final Clock clock;
    private final CurrentUserProvider currentUserProvider;

    public PaymentAuditService(
            PaymentAuditRepository paymentAuditRepository,
            Clock clock,
            CurrentUserProvider currentUserProvider
    ) {
        this.paymentAuditRepository = paymentAuditRepository;
        this.clock = clock;
        this.currentUserProvider = currentUserProvider;
    }

    public void recordStatusChange(
            String paymentReference,
            PaymentStatus previousStatus,
            PaymentStatus newStatus
    ) {

        String performedBy = currentUserProvider.getUsername();
        String branchCode = currentUserProvider.getBranchCode();

        PaymentAudit audit = new PaymentAudit(
                paymentReference,
                PaymentAuditAction.STATUS_CHANGE,
                previousStatus,
                newStatus,
                LocalDateTime.now(clock),
                performedBy,
                branchCode
        );

        paymentAuditRepository.save(audit);
    }
}