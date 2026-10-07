package com.bankpaymentmonitor.paymentservice.payment.alert;

import com.bankpaymentmonitor.paymentservice.config.PaymentMonitoringProperties;
import com.bankpaymentmonitor.paymentservice.payment.alert.dto.PaymentAlertResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.alert.mapper.PaymentAlertMapper;
import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentAlertServiceTest {
    @Mock
    private PaymentAlertRepository paymentAlertRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAlertMapper paymentAlertMapper;

    private PaymentMonitoringProperties monitoringProperties;

    private PaymentAlertService paymentAlertService;

    private Clock clock;

    LocalDateTime now =
            LocalDateTime.of(2026, 10, 1, 12, 0);

    @BeforeEach
    void setUp() {

         clock = Clock.fixed(
                Instant.parse("2026-10-01T12:00:00Z"),
                ZoneOffset.UTC
        );

        monitoringProperties = new PaymentMonitoringProperties();
        monitoringProperties.setPendingThresholdMinutes(5);

        paymentAlertService = new PaymentAlertService(
                paymentRepository,
                paymentAlertRepository,
                monitoringProperties,
                clock,
                paymentAlertMapper
        );
    }

    @Test
    void shouldCreateStalePendingAlert() {

        Payment payment = new Payment(
                "PAY-011",
                "DEMO_SYSTEM",
                "TXN-011",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        PaymentAlert alert =
                paymentAlertService.createStalePendingAlert(payment);

        assertEquals("PAY-011", alert.getPaymentReference());
        assertEquals("BR-001", alert.getBranchCode());
        assertEquals(PaymentAlertType.STALE_PENDING, alert.getType());
        assertEquals(
                LocalDateTime.of(2026, 10, 1, 12, 0),
                alert.getDetectedAt()
        );
    }
    @Test
    void shouldDetectStalePendingAlerts() {

        Payment payment1 = new Payment(
                "PAY-011",
                "DEMO_SYSTEM",
                "TXN-011",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        Payment payment2 = new Payment(
                "PAY-012",
                "DEMO_SYSTEM",
                "TXN-012",
                "BR-002",
                new BigDecimal("250.00"),
                "USD",
                now
        );

        LocalDateTime expectedThreshold =
                LocalDateTime.of(2026, 10, 1, 11, 55);

        when(paymentRepository.findByStatusAndCreatedAtBefore(
                PaymentStatus.PENDING,
                expectedThreshold
        )).thenReturn(List.of(payment1, payment2));

        when(paymentAlertRepository.saveAndFlush(any(PaymentAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<PaymentAlert> alerts =
                paymentAlertService.detectStalePendingAlerts();

        assertEquals(2, alerts.size());

        assertEquals("PAY-011", alerts.get(0).getPaymentReference());
        assertEquals("BR-001", alerts.get(0).getBranchCode());
        assertEquals(
                PaymentAlertType.STALE_PENDING,
                alerts.get(0).getType()
        );

        assertEquals("PAY-012", alerts.get(1).getPaymentReference());
        assertEquals("BR-002", alerts.get(1).getBranchCode());
        assertEquals(
                PaymentAlertType.STALE_PENDING,
                alerts.get(1).getType()
        );

        verify(paymentRepository)
                .findByStatusAndCreatedAtBefore(
                        PaymentStatus.PENDING,
                        expectedThreshold
                );
        verify(paymentAlertRepository, times(2))
                .saveAndFlush(any(PaymentAlert.class));
    }
    @Test
    void shouldNotCreateAlertWhenStalePendingAlertAlreadyExists() {

        Payment payment = new Payment(
                "PAY-011",
                "DEMO_SYSTEM",
                "TXN-011",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        LocalDateTime expectedThreshold =
                LocalDateTime.of(2026, 10, 1, 11, 55);

        when(paymentRepository.findByStatusAndCreatedAtBefore(
                PaymentStatus.PENDING,
                expectedThreshold
        )).thenReturn(List.of(payment));

        when(paymentAlertRepository.existsByPaymentReferenceAndType(
                "PAY-011",
                PaymentAlertType.STALE_PENDING
        )).thenReturn(true);

        List<PaymentAlert> alerts =
                paymentAlertService.detectStalePendingAlerts();

        assertTrue(alerts.isEmpty());

        verify(paymentAlertRepository)
                .existsByPaymentReferenceAndType(
                        "PAY-011",
                        PaymentAlertType.STALE_PENDING
                );

        verify(paymentAlertRepository, never())
                .save(any(PaymentAlert.class));
    }
    @Test
    void shouldIgnoreDuplicateAlertCreatedConcurrently() {

        Payment payment = new Payment(
                "PAY-011",
                "DEMO_SYSTEM",
                "TXN-011",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        LocalDateTime expectedThreshold =
                LocalDateTime.of(2026, 10, 1, 11, 55);

        when(paymentRepository.findByStatusAndCreatedAtBefore(
                PaymentStatus.PENDING,
                expectedThreshold
        )).thenReturn(List.of(payment));

        when(paymentAlertRepository.existsByPaymentReferenceAndType(
                "PAY-011",
                PaymentAlertType.STALE_PENDING
        )).thenReturn(false);

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "Duplicate alert",
                        null,
                        "uk_payment_alert"
                );

        DataIntegrityViolationException dataException =
                new DataIntegrityViolationException(
                        "Duplicate alert",
                        constraintException
                );

        when(paymentAlertRepository.saveAndFlush(
                any(PaymentAlert.class)
        )).thenThrow(dataException);

        List<PaymentAlert> alerts =
                paymentAlertService.detectStalePendingAlerts();

        assertTrue(alerts.isEmpty());

        verify(paymentAlertRepository)
                .saveAndFlush(any(PaymentAlert.class));
    }
    @Test
    void shouldPropagateOtherDatabaseConstraintViolations() {

        Payment payment = new Payment(
                "PAY-011",
                "DEMO_SYSTEM",
                "TXN-011",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        LocalDateTime expectedThreshold =
                LocalDateTime.of(2026, 10, 1, 11, 55);

        when(paymentRepository.findByStatusAndCreatedAtBefore(
                PaymentStatus.PENDING,
                expectedThreshold
        )).thenReturn(List.of(payment));

        when(paymentAlertRepository.existsByPaymentReferenceAndType(
                "PAY-011",
                PaymentAlertType.STALE_PENDING
        )).thenReturn(false);

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "Another database constraint failed",
                        null,
                        "some_other_constraint"
                );

        DataIntegrityViolationException dataException =
                new DataIntegrityViolationException(
                        "Database integrity error",
                        constraintException
                );

        when(paymentAlertRepository.saveAndFlush(
                any(PaymentAlert.class)
        )).thenThrow(dataException);

        DataIntegrityViolationException thrown =
                assertThrows(
                        DataIntegrityViolationException.class,
                        () -> paymentAlertService.detectStalePendingAlerts()
                );

        assertSame(dataException, thrown);
    }
    @Test
    void shouldResolveAllOpenAlertsForPayment() {

        String paymentReference = "PAY-TEST-100";

        LocalDateTime detectedAt =
                LocalDateTime.of(2026, 10, 2, 11, 50);

        PaymentAlert firstAlert = new PaymentAlert(
                paymentReference,
                "BR-001",
                PaymentAlertType.STALE_PENDING,
                detectedAt
        );

        PaymentAlert secondAlert = new PaymentAlert(
                paymentReference,
                "BR-001",
                PaymentAlertType.STALE_PENDING,
                detectedAt
        );

        when(
                paymentAlertRepository
                        .findByPaymentReferenceAndStatus(
                                paymentReference,
                                PaymentAlertStatus.OPEN
                        )
        ).thenReturn(List.of(firstAlert, secondAlert));

        List<PaymentAlert> resolvedAlerts =
                paymentAlertService.resolveOpenAlerts(
                        paymentReference
                );

        assertEquals(2, resolvedAlerts.size());

        LocalDateTime expectedResolutionTime =
                LocalDateTime.now(clock);

        assertEquals(
                PaymentAlertStatus.RESOLVED,
                firstAlert.getStatus()
        );

        assertEquals(
                expectedResolutionTime,
                firstAlert.getResolvedAt()
        );

        assertEquals(
                PaymentAlertStatus.RESOLVED,
                secondAlert.getStatus()
        );

        assertEquals(
                expectedResolutionTime,
                secondAlert.getResolvedAt()
        );

        verify(paymentAlertRepository)
                .findByPaymentReferenceAndStatus(
                        paymentReference,
                        PaymentAlertStatus.OPEN
                );
    }
    @Test
    void shouldGetAlertsByBranch() {

        LocalDateTime detectedAt =
                LocalDateTime.of(2026, 10, 2, 10, 0);

        PaymentAlert alert = new PaymentAlert(
                "PAY-001",
                "BR-001",
                PaymentAlertType.STALE_PENDING,
                detectedAt
        );

        PaymentAlertResponseDTO dto =
                new PaymentAlertResponseDTO(
                        null,
                        "PAY-001",
                        "BR-001",
                        "STALE_PENDING",
                        "OPEN",
                        detectedAt,
                        null
                );

        when(paymentAlertRepository.findByBranchCode("BR-001"))
                .thenReturn(List.of(alert));

        when(paymentAlertMapper.toResponseDTO(alert))
                .thenReturn(dto);

        List<PaymentAlertResponseDTO> result =
                paymentAlertService.getAlertsByBranch("BR-001");

        assertEquals(1, result.size());

        assertEquals(
                "PAY-001",
                result.get(0).paymentReference()
        );

        assertEquals(
                "BR-001",
                result.get(0).branchCode()
        );

        assertEquals(
                "OPEN",
                result.get(0).status()
        );

        verify(paymentAlertRepository)
                .findByBranchCode("BR-001");

        verify(paymentAlertMapper)
                .toResponseDTO(alert);
    }
    @Test
    void shouldGetAlertsByBranchAndStatus() {

        LocalDateTime detectedAt =
                LocalDateTime.of(2026, 10, 2, 10, 0);

        PaymentAlert alert = new PaymentAlert(
                "PAY-001",
                "BR-001",
                PaymentAlertType.STALE_PENDING,
                detectedAt
        );

        PaymentAlertResponseDTO dto =
                new PaymentAlertResponseDTO(
                        null,
                        "PAY-001",
                        "BR-001",
                        "STALE_PENDING",
                        "OPEN",
                        detectedAt,
                        null
                );

        when(
                paymentAlertRepository.findByBranchCodeAndStatus(
                        "BR-001",
                        PaymentAlertStatus.OPEN
                )
        ).thenReturn(List.of(alert));

        when(paymentAlertMapper.toResponseDTO(alert))
                .thenReturn(dto);

        List<PaymentAlertResponseDTO> result =
                paymentAlertService.getAlertsByBranchAndStatus(
                        "BR-001",
                        PaymentAlertStatus.OPEN
                );

        assertEquals(1, result.size());

        assertEquals(
                "PAY-001",
                result.get(0).paymentReference()
        );

        assertEquals(
                "BR-001",
                result.get(0).branchCode()
        );

        assertEquals(
                "OPEN",
                result.get(0).status()
        );

        verify(paymentAlertRepository)
                .findByBranchCodeAndStatus(
                        "BR-001",
                        PaymentAlertStatus.OPEN
                );

        verify(paymentAlertMapper)
                .toResponseDTO(alert);
    }
    @Test
    void shouldGetAlertsByPaymentReference() {

        LocalDateTime detectedAt =
                LocalDateTime.of(2026, 10, 5, 9, 0);

        PaymentAlert alert = new PaymentAlert(
                "PAY-001",
                "BR-001",
                PaymentAlertType.STALE_PENDING,
                detectedAt
        );

        PaymentAlertResponseDTO dto =
                new PaymentAlertResponseDTO(
                        null,
                        "PAY-001",
                        "BR-001",
                        "STALE_PENDING",
                        "OPEN",
                        detectedAt,
                        null
                );

        when(
                paymentAlertRepository.findByPaymentReference(
                        "PAY-001"
                )
        ).thenReturn(List.of(alert));

        when(paymentAlertMapper.toResponseDTO(alert))
                .thenReturn(dto);

        List<PaymentAlertResponseDTO> result =
                paymentAlertService.getAlertsByPaymentReference(
                        "PAY-001"
                );

        assertEquals(1, result.size());

        assertEquals(
                "PAY-001",
                result.get(0).paymentReference()
        );

        assertEquals(
                "BR-001",
                result.get(0).branchCode()
        );

        assertEquals(
                "STALE_PENDING",
                result.get(0).type()
        );

        assertEquals(
                "OPEN",
                result.get(0).status()
        );

        verify(paymentAlertRepository)
                .findByPaymentReference("PAY-001");

        verify(paymentAlertMapper)
                .toResponseDTO(alert);
    }
}