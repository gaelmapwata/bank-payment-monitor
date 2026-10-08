package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.exception.PaymentNotFoundException;
import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import com.bankpaymentmonitor.paymentservice.payment.security.CurrentUserProvider;
import com.bankpaymentmonitor.paymentservice.payment.security.Role;
import com.bankpaymentmonitor.paymentservice.payment.audit.dto.PaymentAuditResponseDTO;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentAuditQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentAuditRepository paymentAuditRepository;
    private final CurrentUserProvider currentUserProvider;

    public PaymentAuditQueryService(
            PaymentRepository paymentRepository,
            PaymentAuditRepository paymentAuditRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAuditRepository = paymentAuditRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<PaymentAuditResponseDTO> getAuditsByPaymentReference(
            String paymentReference
    ) {

        // 1. Rechercher le paiement
        Payment payment = paymentRepository
                .findByReference(paymentReference)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentReference)
                );

        // 2. Récupérer le rôle de l'utilisateur
        Role role = currentUserProvider.getRole();

        // 3. Vérifier les droits d'accès
        if (role != Role.ADMIN) {

            String userBranchCode =
                    currentUserProvider.getBranchCode();

            if (role != Role.USER
                    || userBranchCode == null
                    || !userBranchCode.equals(payment.getBranchCode())) {

                throw new AccessDeniedException(
                        "You are not allowed to access this payment's audits"
                );
            }
        }

        // 4. Charger les audits seulement après autorisation
        return paymentAuditRepository
                .findByPaymentReferenceOrderByOccurredAtAsc(
                        paymentReference
                )
                .stream()
                .map(PaymentAuditMapper::toResponseDTO)
                .toList();
    }
}