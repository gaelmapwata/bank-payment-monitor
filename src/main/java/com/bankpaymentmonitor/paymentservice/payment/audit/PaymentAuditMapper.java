package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.audit.dto.PaymentAuditResponseDTO;

public final class PaymentAuditMapper {

    private PaymentAuditMapper() {
        // Empêche l'instanciation de cette classe utilitaire
    }

    public static PaymentAuditResponseDTO toResponseDTO(
            PaymentAudit audit
    ) {
        return new PaymentAuditResponseDTO(
                audit.getId(),
                audit.getPaymentReference(),
                audit.getAction(),
                audit.getPreviousStatus(),
                audit.getNewStatus(),
                audit.getPerformedBy(),
                audit.getBranchCode(),
                audit.getOccurredAt()
        );
    }
}