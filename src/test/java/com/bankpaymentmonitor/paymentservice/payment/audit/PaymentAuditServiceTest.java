package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.security.CurrentUserProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAuditServiceTest {

    @Mock
    private PaymentAuditRepository paymentAuditRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private PaymentAuditService paymentAuditService;

    private Clock clock;

    @BeforeEach
    void setUp() {

        clock = Clock.fixed(
                Instant.parse("2026-10-07T15:30:00Z"),
                ZoneOffset.UTC
        );

        paymentAuditService = new PaymentAuditService(
                paymentAuditRepository,
                clock,
                currentUserProvider
        );
    }

    @Test
    void shouldRecordStatusChangeAudit() {

        // GIVEN : utilisateur authentifié
        when(currentUserProvider.getUsername())
                .thenReturn("john");

        when(currentUserProvider.getBranchCode())
                .thenReturn("BR-001");

        // WHEN : enregistrement du changement de statut
        paymentAuditService.recordStatusChange(
                "PAY-2026-001",
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING
        );

        // THEN : capturer l'audit enregistré
        ArgumentCaptor<PaymentAudit> auditCaptor =
                ArgumentCaptor.forClass(PaymentAudit.class);

        verify(paymentAuditRepository)
                .save(auditCaptor.capture());

        PaymentAudit capturedAudit =
                auditCaptor.getValue();

        assertEquals(
                "PAY-2026-001",
                capturedAudit.getPaymentReference()
        );

        assertEquals(
                PaymentAuditAction.STATUS_CHANGE,
                capturedAudit.getAction()
        );

        assertEquals(
                PaymentStatus.PENDING,
                capturedAudit.getPreviousStatus()
        );

        assertEquals(
                PaymentStatus.PROCESSING,
                capturedAudit.getNewStatus()
        );

        assertEquals(
                LocalDateTime.of(2026, 10, 7, 15, 30),
                capturedAudit.getOccurredAt()
        );

        // Nouvelles vérifications : auteur et agence
        assertEquals(
                "john",
                capturedAudit.getPerformedBy()
        );

        assertEquals(
                "BR-001",
                capturedAudit.getBranchCode()
        );
    }
    @Test
    void shouldRecordStatusChangeAuditForAdminWithoutBranch() {

        // GIVEN : un administrateur du siège
        when(currentUserProvider.getUsername())
                .thenReturn("admin.hq");

        when(currentUserProvider.getBranchCode())
                .thenReturn(null);

        // WHEN : l'administrateur change le statut
        paymentAuditService.recordStatusChange(
                "PAY-ADMIN-001",
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING
        );

        // THEN : récupérer l'audit enregistré
        ArgumentCaptor<PaymentAudit> auditCaptor =
                ArgumentCaptor.forClass(PaymentAudit.class);

        verify(paymentAuditRepository)
                .save(auditCaptor.capture());

        PaymentAudit savedAudit = auditCaptor.getValue();

        // Vérifier l'identité de l'administrateur
        assertEquals("admin.hq", savedAudit.getPerformedBy());

        // Un administrateur du siège n'a pas d'agence
        assertNull(savedAudit.getBranchCode());
    }
}