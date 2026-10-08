package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.audit.dto.PaymentAuditResponseDTO;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentAuditController {

    private final PaymentAuditQueryService paymentAuditQueryService;

    public PaymentAuditController(
            PaymentAuditQueryService paymentAuditQueryService
    ) {
        this.paymentAuditQueryService = paymentAuditQueryService;
    }

    @GetMapping("/{reference}/audits")
    public List<PaymentAuditResponseDTO> getAudits(
            @PathVariable String reference
    ) {
        return paymentAuditQueryService
                .getAuditsByPaymentReference(reference);
    }
}