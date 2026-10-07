package com.bankpaymentmonitor.paymentservice.payment.controller;

import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentCreateDTO;
import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.security.CustomUserPrincipal;
import com.bankpaymentmonitor.paymentservice.payment.security.Role;
import com.bankpaymentmonitor.paymentservice.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{reference}")
    @PreAuthorize(
            "@branchAccessService.canAccessPayment(authentication.principal, #reference)"
    )
    public PaymentResponseDTO getPaymentByReference(
            @PathVariable String reference
    ) {
        return paymentService.getPaymentByReference(reference);
    }
    @GetMapping("/branch/{branchCode}")
    @PreAuthorize(
            "@branchAccessService.canAccessBranch(authentication.principal, #branchCode)"
    )
    public List<PaymentResponseDTO> getPaymentsByBranch(
            @PathVariable String branchCode
    ) {
        return paymentService.getPaymentsByBranch(branchCode);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(
            "@branchAccessService.canAccessBranch(authentication.principal, #request.branchCode())"
    )
    public PaymentResponseDTO createPayment(
            @Valid @RequestBody PaymentCreateDTO request
    ) {
        return paymentService.createPayment(request);
    }
    @PatchMapping("/{reference}/processing")
    @PreAuthorize(
            "@branchAccessService.canAccessPayment(authentication.principal, #reference)"
    )
    public PaymentResponseDTO markAsProcessing(
            @PathVariable String reference
    ) {
        return paymentService.markAsProcessing(reference);
    }
    @PatchMapping("/{reference}/success")
    @PreAuthorize(
            "@branchAccessService.canAccessPayment(authentication.principal, #reference)"
    )
    public PaymentResponseDTO markAsSuccess(
            @PathVariable String reference
    ) {
        return paymentService.markAsSuccess(reference);
    }
    @PatchMapping("/{reference}/failed")
    @PreAuthorize(
            "@branchAccessService.canAccessPayment(authentication.principal, #reference)"
    )
    public PaymentResponseDTO markAsFailed(
            @PathVariable String reference
    ) {
        return paymentService.markAsFailed(reference);
    }
    @GetMapping("/status/{status}")
    public List<PaymentResponseDTO> getPaymentsByStatus(
            @PathVariable PaymentStatus status,
            Authentication authentication
    ) {

        CustomUserPrincipal principal = getPrincipal(authentication);

        if (principal.getRole() == Role.ADMIN) {
            return paymentService.getPaymentsByStatus(status);
        }

        return paymentService.getPaymentsByStatusAndBranch(
                status,
                principal.getBranchCode()
        );
    }
    @GetMapping("/pending/stale")
    public List<PaymentResponseDTO> getStalePendingPayments(
            Authentication authentication
    ) {

        CustomUserPrincipal principal = getPrincipal(authentication);

        if (principal.getRole() == Role.ADMIN) {
            return paymentService.getStalePendingPayments();
        }

        return paymentService.getStalePendingPaymentsForBranch(
                principal.getBranchCode()
        );
    }
    private CustomUserPrincipal getPrincipal(
            Authentication authentication
    ) {
        return (CustomUserPrincipal) authentication.getPrincipal();
    }
}