package com.bankpaymentmonitor.paymentservice.payment.alert.controller;

import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertService;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertStatus;
import com.bankpaymentmonitor.paymentservice.payment.alert.dto.PaymentAlertResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alerts")
public class PaymentAlertController {

    private final PaymentAlertService paymentAlertService;

    public PaymentAlertController(
            PaymentAlertService paymentAlertService
    ) {
        this.paymentAlertService = paymentAlertService;
    }

    @GetMapping("/branch/{branchCode}")
    @PreAuthorize(
            "@branchAccessService.canAccessBranch(authentication.principal, #branchCode)"
    )
    public ResponseEntity<List<PaymentAlertResponseDTO>> getAlertsByBranch(
            @PathVariable String branchCode
    ) {
        return ResponseEntity.ok(
                paymentAlertService.getAlertsByBranch(branchCode)
        );
    }
    @GetMapping("/branch/{branchCode}/status/{status}")
    @PreAuthorize(
            "@branchAccessService.canAccessBranch(authentication.principal, #branchCode)"
    )
    public ResponseEntity<List<PaymentAlertResponseDTO>> getAlertsByBranchAndStatus(
            @PathVariable String branchCode,
            @PathVariable PaymentAlertStatus status
    ) {
        return ResponseEntity.ok(
                paymentAlertService.getAlertsByBranchAndStatus(
                        branchCode,
                        status
                )
        );
    }
    @GetMapping("/payment/{paymentReference}")
    @PreAuthorize(
            "@branchAccessService.canAccessPaymentAlerts(authentication.principal, #paymentReference)"
    )
    public ResponseEntity<List<PaymentAlertResponseDTO>> getAlertsByPaymentReference(
            @PathVariable String paymentReference
    ) {
        return ResponseEntity.ok(
                paymentAlertService.getAlertsByPaymentReference(paymentReference)
        );
    }
}