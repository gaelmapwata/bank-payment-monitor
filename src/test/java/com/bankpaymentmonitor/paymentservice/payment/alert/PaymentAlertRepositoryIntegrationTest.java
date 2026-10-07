package com.bankpaymentmonitor.paymentservice.payment.alert;

import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.time.Clock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Testcontainers
@SpringBootTest
@Transactional
@Import(PaymentAlertServiceIntegrationTest.FixedClockConfig.class)
class PaymentAlertServiceIntegrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17")
                    .withDatabaseName("payment_test_db")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(
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

    @Autowired
    private PaymentAlertService paymentAlertService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private PaymentAlertRepository paymentAlertRepository;

    @Test
    void shouldDetectStalePendingPaymentAndCreateAlert() {

        LocalDateTime creationTime =
                LocalDateTime.now(clock).minusMinutes(10);

        Payment payment = new Payment(
                "PAY-INT-100",
                "DEMO_SYSTEM",
                "TXN-INT-100",
                "BR-001",
                new BigDecimal("250.00"),
                "USD",
                creationTime
        );

        paymentRepository.saveAndFlush(payment);

        List<PaymentAlert> alerts =
                paymentAlertService.detectStalePendingAlerts();

        assertEquals(1, alerts.size());

        PaymentAlert alert = alerts.get(0);

        assertEquals(
                "PAY-INT-100",
                alert.getPaymentReference()
        );

        assertEquals(
                "BR-001",
                alert.getBranchCode()
        );

        assertEquals(
                PaymentAlertType.STALE_PENDING,
                alert.getType()
        );

        assertNotNull(alert.getId());
        List<PaymentAlert> persistedAlerts =
                paymentAlertRepository.findByPaymentReference("PAY-INT-100");

        assertEquals(1, persistedAlerts.size());

        PaymentAlert persistedAlert = persistedAlerts.get(0);

        assertEquals(
                "PAY-INT-100",
                persistedAlert.getPaymentReference()
        );

        assertEquals(
                PaymentAlertType.STALE_PENDING,
                persistedAlert.getType()
        );
    }
    @Test
    void shouldNotCreateDuplicateStalePendingAlert() {

        LocalDateTime creationTime =
                LocalDateTime.now(clock).minusMinutes(10);

        Payment payment = new Payment(
                "PAY-INT-200",
                "DEMO_SYSTEM",
                "TXN-INT-200",
                "BR-001",
                new BigDecimal("300.00"),
                "USD",
                creationTime
        );

        paymentRepository.saveAndFlush(payment);

        // Première détection
        List<PaymentAlert> firstDetection =
                paymentAlertService.detectStalePendingAlerts();

        assertEquals(1, firstDetection.size());

        // Deuxième détection
        List<PaymentAlert> secondDetection =
                paymentAlertService.detectStalePendingAlerts();

        assertTrue(secondDetection.isEmpty());

        // Vérification finale directement dans PostgreSQL
        List<PaymentAlert> persistedAlerts =
                paymentAlertRepository.findByPaymentReference(
                        "PAY-INT-200"
                );

        assertEquals(1, persistedAlerts.size());
    }
    @Test
    void shouldNotCreateAlertForRecentPendingPayment() {

        LocalDateTime creationTime =
                LocalDateTime.now(clock).minusMinutes(2);

        Payment payment = new Payment(
                "PAY-INT-300",
                "DEMO_SYSTEM",
                "TXN-INT-300",
                "BR-001",
                new BigDecimal("200.00"),
                "USD",
                creationTime
        );

        paymentRepository.saveAndFlush(payment);

        List<PaymentAlert> alerts =
                paymentAlertService.detectStalePendingAlerts();

        assertTrue(alerts.isEmpty());

        List<PaymentAlert> persistedAlerts =
                paymentAlertRepository.findByPaymentReference(
                        "PAY-INT-300"
                );

        assertTrue(persistedAlerts.isEmpty());
    }
    @Test
    void shouldNotCreateAlertWhenPendingForExactlyThreshold() {

        LocalDateTime creationTime =
                LocalDateTime.now(clock).minusMinutes(5);

        Payment payment = new Payment(
                "PAY-INT-400",
                "DEMO_SYSTEM",
                "TXN-INT-400",
                "BR-001",
                new BigDecimal("400.00"),
                "USD",
                creationTime
        );

        paymentRepository.saveAndFlush(payment);

        List<PaymentAlert> alerts =
                paymentAlertService.detectStalePendingAlerts();

        assertTrue(alerts.isEmpty());

        List<PaymentAlert> persistedAlerts =
                paymentAlertRepository.findByPaymentReference(
                        "PAY-INT-400"
                );

        assertTrue(persistedAlerts.isEmpty());
    }
    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    Instant.parse("2026-10-02T12:00:00Z"),
                    ZoneOffset.UTC
            );
        }
    }
    @Test
    void shouldCreateAlertWhenPendingBeyondThreshold() {

        LocalDateTime creationTime =
                LocalDateTime.now(clock)
                        .minusMinutes(5)
                        .minusSeconds(1);

        Payment payment = new Payment(
                "PAY-INT-500",
                "DEMO_SYSTEM",
                "TXN-INT-500",
                "BR-001",
                new BigDecimal("500.00"),
                "USD",
                creationTime
        );

        paymentRepository.saveAndFlush(payment);

        List<PaymentAlert> alerts =
                paymentAlertService.detectStalePendingAlerts();

        assertEquals(1, alerts.size());

        PaymentAlert alert = alerts.get(0);

        assertEquals(
                "PAY-INT-500",
                alert.getPaymentReference()
        );

        assertEquals(
                PaymentAlertType.STALE_PENDING,
                alert.getType()
        );

        List<PaymentAlert> persistedAlerts =
                paymentAlertRepository.findByPaymentReference(
                        "PAY-INT-500"
                );

        assertEquals(1, persistedAlerts.size());
    }
}