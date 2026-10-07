package com.bankpaymentmonitor.paymentservice.payment.service;

import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlert;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertRepository;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertStatus;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertType;
import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
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
}