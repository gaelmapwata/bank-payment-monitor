package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.audit.dto.PaymentAuditResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.exception.PaymentNotFoundException;
import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import com.bankpaymentmonitor.paymentservice.payment.security.CurrentUserProvider;
import com.bankpaymentmonitor.paymentservice.payment.security.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAuditQueryServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAuditRepository paymentAuditRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private PaymentAuditQueryService paymentAuditQueryService;

    @BeforeEach
    void setUp() {
        paymentAuditQueryService = new PaymentAuditQueryService(
                paymentRepository,
                paymentAuditRepository,
                currentUserProvider
        );
    }

    @Test
    void shouldRejectUserFromAnotherBranch() {

        // GIVEN : utilisateur de BR-001
        when(currentUserProvider.getRole())
                .thenReturn(Role.USER);

        when(currentUserProvider.getBranchCode())
                .thenReturn("BR-001");

        // Le paiement appartient à BR-002
        Payment payment = mock(Payment.class);

        when(payment.getBranchCode())
                .thenReturn("BR-002");

        when(paymentRepository.findByReference("PAY-001"))
                .thenReturn(Optional.of(payment));

        // WHEN / THEN : accès interdit
        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> paymentAuditQueryService.getAuditsByPaymentReference(
                        "PAY-001"
                )
        );

        // Aucun audit ne doit être consulté
        verifyNoInteractions(paymentAuditRepository);
    }
    @Test
    void shouldAllowUserToReadAuditsFromOwnBranch() {

        // GIVEN : utilisateur de BR-001
        when(currentUserProvider.getRole())
                .thenReturn(Role.USER);

        when(currentUserProvider.getBranchCode())
                .thenReturn("BR-001");

        // Le paiement appartient à la même agence
        Payment payment = mock(Payment.class);

        when(payment.getBranchCode())
                .thenReturn("BR-001");

        when(paymentRepository.findByReference("PAY-001"))
                .thenReturn(Optional.of(payment));

        // Un audit existant
        PaymentAudit audit = new PaymentAudit(
                "PAY-001",
                PaymentAuditAction.STATUS_CHANGE,
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING,
                LocalDateTime.of(2026, 10, 8, 12, 0),
                "john",
                "BR-001"
        );

        when(paymentAuditRepository
                .findByPaymentReferenceOrderByOccurredAtAsc("PAY-001"))
                .thenReturn(List.of(audit));

        // WHEN : consultation de l'historique
        List<PaymentAuditResponseDTO> result =
                paymentAuditQueryService
                        .getAuditsByPaymentReference("PAY-001");

        // THEN : accès autorisé
        assertEquals(1, result.size());

        PaymentAuditResponseDTO dto = result.get(0);

        assertEquals("PAY-001", dto.paymentReference());
        assertEquals(PaymentStatus.PENDING, dto.previousStatus());
        assertEquals(PaymentStatus.PROCESSING, dto.newStatus());
        assertEquals("john", dto.performedBy());
        assertEquals("BR-001", dto.branchCode());

        verify(paymentAuditRepository)
                .findByPaymentReferenceOrderByOccurredAtAsc("PAY-001");
    }
    @Test
    void shouldAllowAdminToReadAuditsFromAnyBranch() {

        // GIVEN : administrateur sans agence
        when(currentUserProvider.getRole())
                .thenReturn(Role.ADMIN);

        // Paiement appartenant à BR-002
        Payment payment = mock(Payment.class);

        when(paymentRepository.findByReference("PAY-002"))
                .thenReturn(Optional.of(payment));

        // Audit du paiement
        PaymentAudit audit = new PaymentAudit(
                "PAY-002",
                PaymentAuditAction.STATUS_CHANGE,
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING,
                LocalDateTime.of(2026, 10, 8, 12, 30),
                "agent.br002",
                "BR-002"
        );

        when(paymentAuditRepository
                .findByPaymentReferenceOrderByOccurredAtAsc("PAY-002"))
                .thenReturn(List.of(audit));

        // WHEN
        List<PaymentAuditResponseDTO> result =
                paymentAuditQueryService
                        .getAuditsByPaymentReference("PAY-002");

        // THEN
        assertEquals(1, result.size());

        assertEquals("PAY-002", result.get(0).paymentReference());
        assertEquals("BR-002", result.get(0).branchCode());

        // Un ADMIN ne doit pas avoir besoin de son agence
        verify(currentUserProvider, never()).getBranchCode();

        verify(paymentAuditRepository)
                .findByPaymentReferenceOrderByOccurredAtAsc("PAY-002");
    }
    @Test
    void shouldThrowPaymentNotFoundExceptionWhenPaymentDoesNotExist() {

        // GIVEN
        String reference = "PAY-UNKNOWN";

        when(paymentRepository.findByReference(reference))
                .thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(
                PaymentNotFoundException.class,
                () -> paymentAuditQueryService
                        .getAuditsByPaymentReference(reference)
        );

        // Aucun audit ne doit être consulté
        verifyNoInteractions(paymentAuditRepository);
        verifyNoInteractions(currentUserProvider);
    }
}