package com.bankpaymentmonitor.paymentservice.payment.service;

import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlert;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertRepository;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertStatus;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertType;
import com.bankpaymentmonitor.paymentservice.payment.audit.PaymentAuditRepository;
import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.audit.PaymentAudit;
import com.bankpaymentmonitor.paymentservice.payment.audit.PaymentAuditAction;
import com.bankpaymentmonitor.paymentservice.payment.security.CustomUserPrincipal;
import com.bankpaymentmonitor.paymentservice.payment.security.AppUser;
import com.bankpaymentmonitor.paymentservice.payment.security.Role;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.bankpaymentmonitor.paymentservice.payment.audit.PaymentAuditRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentAlertRepository paymentAlertRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PaymentAuditRepository paymentAuditRepository;

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17")
                    .withDatabaseName("payment_test_db")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldStartPostgreSQLContainer() {
        assertTrue(postgres.isRunning());
    }
    @Test
    void shouldLoadPaymentService() {
        assertNotNull(paymentService);
    }
    @Test
    void shouldPersistPendingPayment() {

        LocalDateTime now = LocalDateTime.now();

        Payment payment = new Payment(
                "PAY-INT-001",
                "DEMO_SYSTEM",
                "INT-TXN-001",
                "BR-001",
                new BigDecimal("250.00"),
                "USD",
                now
        );

        paymentRepository.saveAndFlush(payment);

        Payment savedPayment =
                paymentRepository.findByReference("PAY-INT-001")
                        .orElseThrow();

        assertEquals(
                PaymentStatus.PENDING,
                savedPayment.getStatus()
        );

        assertEquals(
                "PAY-INT-001",
                savedPayment.getReference()
        );
    }
    @Test
    void shouldPersistOpenAlertForPayment() {

        LocalDateTime now = LocalDateTime.now();

        Payment payment = new Payment(
                "PAY-INT-002",
                "DEMO_SYSTEM",
                "INT-TXN-002",
                "BR-001",
                new BigDecimal("250.00"),
                "USD",
                now
        );

        paymentRepository.saveAndFlush(payment);

        PaymentAlert alert = new PaymentAlert(
                payment.getReference(),
                payment.getBranchCode(),
                PaymentAlertType.STALE_PENDING,
                now
        );

        paymentAlertRepository.saveAndFlush(alert);

        List<PaymentAlert> alerts =
                paymentAlertRepository.findByPaymentReference(
                        payment.getReference()
                );

        assertEquals(1, alerts.size());

        PaymentAlert savedAlert = alerts.get(0);

        assertEquals(
                PaymentAlertStatus.OPEN,
                savedAlert.getStatus()
        );

        assertNull(savedAlert.getResolvedAt());
    }
    @Test
    void shouldResolveOpenAlertWhenPaymentMovesToProcessing() {

        LocalDateTime now = LocalDateTime.now();

        // 1. Créer un paiement PENDING
        Payment payment = new Payment(
                "PAY-INT-003",
                "DEMO_SYSTEM",
                "INT-TXN-003",
                "BR-001",
                new BigDecimal("250.00"),
                "USD",
                now
        );

        paymentRepository.saveAndFlush(payment);

        // 2. Créer une alerte OPEN pour ce paiement
        PaymentAlert alert = new PaymentAlert(
                payment.getReference(),
                payment.getBranchCode(),
                PaymentAlertType.STALE_PENDING,
                now
        );

        paymentAlertRepository.saveAndFlush(alert);
        authenticateTestUser();

        // 3. Appeler le vrai PaymentService
        PaymentResponseDTO response =
                paymentService.markAsProcessing(
                        payment.getReference()
                );
        entityManager.flush();
        entityManager.clear();

        // 4. Vérifier le paiement
        assertEquals(
                "PROCESSING",
                response.status()
        );

        Payment updatedPayment =
                paymentRepository.findByReference(
                        payment.getReference()
                ).orElseThrow();

        assertEquals(
                PaymentStatus.PROCESSING,
                updatedPayment.getStatus()
        );

        // 5. Vérifier l'alerte
        List<PaymentAlert> alerts =
                paymentAlertRepository.findByPaymentReference(
                        payment.getReference()
                );

        assertEquals(1, alerts.size());

        PaymentAlert resolvedAlert = alerts.get(0);

        assertEquals(
                PaymentAlertStatus.RESOLVED,
                resolvedAlert.getStatus()
        );

        assertNotNull(
                resolvedAlert.getResolvedAt()
        );
    }
    @Test
    void shouldPersistAuditWhenPaymentMovesToProcessing() {

        // GIVEN : créer un paiement avec le statut PENDING
        LocalDateTime now = LocalDateTime.now();

        Payment payment = new Payment(
                "PAY-INT-AUDIT-001",
                "DEMO_SYSTEM",
                "INT-AUDIT-TXN-001",
                "BR-001",
                new BigDecimal("250.00"),
                "USD",
                now
        );

        paymentRepository.saveAndFlush(payment);

        // Simuler un utilisateur authentifié de l'agence BR-001
        authenticateTestUser();

        // WHEN : effectuer une transition PENDING -> PROCESSING
        PaymentResponseDTO response =
                paymentService.markAsProcessing(
                        payment.getReference()
                );

        // Forcer l'écriture SQL puis vider le contexte JPA
        entityManager.flush();
        entityManager.clear();

        // THEN : vérifier la réponse du service
        assertEquals(
                PaymentStatus.PROCESSING.name(),
                response.status()
        );

        // Récupérer les audits réellement enregistrés en base
        List<PaymentAudit> audits =
                paymentAuditRepository
                        .findByPaymentReferenceOrderByOccurredAtAsc(
                                payment.getReference()
                        );

        // Une seule transition doit produire un seul audit
        assertEquals(1, audits.size());

        PaymentAudit savedAudit = audits.get(0);

        // Vérifier la référence du paiement
        assertEquals(
                payment.getReference(),
                savedAudit.getPaymentReference()
        );

        // Vérifier le type d'action
        assertEquals(
                PaymentAuditAction.STATUS_CHANGE,
                savedAudit.getAction()
        );

        // Vérifier les statuts avant et après
        assertEquals(
                PaymentStatus.PENDING,
                savedAudit.getPreviousStatus()
        );

        assertEquals(
                PaymentStatus.PROCESSING,
                savedAudit.getNewStatus()
        );

        // Vérifier l'utilisateur qui a effectué l'opération
        assertEquals(
                "john",
                savedAudit.getPerformedBy()
        );

        // Vérifier l'agence de l'utilisateur
        assertEquals(
                "BR-001",
                savedAudit.getBranchCode()
        );

        // Vérifier que la date de l'audit existe
        assertNotNull(savedAudit.getOccurredAt());
    }
    private void authenticateTestUser() {

        AppUser user = new AppUser(
                "john",
                "encoded-password",
                "BR-001",
                Role.USER,
                true
        );

        CustomUserPrincipal principal =
                new CustomUserPrincipal(user);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }
}