package com.bankpaymentmonitor.paymentservice.payment.service;

import com.bankpaymentmonitor.paymentservice.config.PaymentMonitoringProperties;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertService;
import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentCreateDTO;
import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.exception.InvalidPaymentStatusTransitionException;
import com.bankpaymentmonitor.paymentservice.payment.exception.PaymentAlreadyExistsException;
import com.bankpaymentmonitor.paymentservice.payment.exception.PaymentNotFoundException;
import com.bankpaymentmonitor.paymentservice.payment.mapper.PaymentMapper;
import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    private PaymentMapper paymentMapper;
    private PaymentMonitoringProperties monitoringProperties;

    @Mock
    private PaymentAlertService paymentAlertService;

    LocalDateTime now =
            LocalDateTime.of(2026, 10, 1, 12, 0);


    private PaymentService paymentService;
    @BeforeEach
    void setUp() {

        paymentMapper = new PaymentMapper();

        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-10-01T12:00:00Z"),
                ZoneOffset.UTC
        );

        monitoringProperties = new PaymentMonitoringProperties();
        monitoringProperties.setPendingThresholdMinutes(5);



        paymentService = new PaymentService(
                monitoringProperties,
                paymentRepository,
                paymentMapper,
                fixedClock,
                paymentAlertService
        );
    }

    @Test
    void shouldReturnPaymentWhenReferenceExists() {
        String reference = "PAY-2026-0001";

        Payment payment = mock(Payment.class);

        when(paymentRepository.findByReference(reference))
                .thenReturn(Optional.of(payment));

        when(payment.getReference()).thenReturn(reference);
        when(payment.getAmount()).thenReturn(new BigDecimal("150.00"));
        when(payment.getCurrency()).thenReturn("USD");
        when(payment.getStatus()).thenReturn(PaymentStatus.SUCCESS);

        PaymentResponseDTO response =
                paymentService.getPaymentByReference(reference);

        assertEquals(reference, response.reference());
        assertEquals(new BigDecimal("150.00"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals("SUCCESS", response.status());

        verify(paymentRepository).findByReference(reference);
    }

    @Test
    void shouldThrowExceptionWhenReferenceDoesNotExist() {
        String reference = "PAY-2026-9999";

        when(paymentRepository.findByReference(reference))
                .thenReturn(Optional.empty());

        assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.getPaymentByReference(reference)
        );

        verify(paymentRepository).findByReference(reference);
    }

    @Test
    void shouldRejectPaymentWhenSourceReferenceAlreadyExists() {
        PaymentCreateDTO request = new PaymentCreateDTO(
                "DEMO_SYSTEM",
                "DEMO-TXN-0002",
                "BR-001",
                new BigDecimal("75.50"),
                "USD"
        );

        when(paymentRepository.existsBySourceSystemAndSourcePaymentReference(
                "DEMO_SYSTEM",
                "DEMO-TXN-0002"
        )).thenReturn(true);

        assertThrows(
                PaymentAlreadyExistsException.class,
                () -> paymentService.createPayment(request)
        );

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldCreatePaymentWhenSourceReferenceDoesNotExist() {
        PaymentCreateDTO request = new PaymentCreateDTO(
                "DEMO_SYSTEM",
                "DEMO-TXN-0004",
                "BR-001",
                new BigDecimal("75.50"),
                "USD"
        );

        when(paymentRepository.existsBySourceSystemAndSourcePaymentReference(
                "DEMO_SYSTEM",
                "DEMO-TXN-0004"
        )).thenReturn(false);

        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponseDTO response = paymentService.createPayment(request);

        assertNotNull(response.reference());
        assertEquals(new BigDecimal("75.50"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals("PENDING", response.status());

        verify(paymentRepository).saveAndFlush(any(Payment.class));
    }

    @Test
    void shouldRejectDuplicateWhenDatabaseDetectsConflict() {
        PaymentCreateDTO request = new PaymentCreateDTO(
                "DEMO_SYSTEM",
                "DEMO-TXN-0005",
                "BR-001",
                new BigDecimal("75.50"),
                "USD"
        );

        when(paymentRepository.existsBySourceSystemAndSourcePaymentReference(
                "DEMO_SYSTEM",
                "DEMO-TXN-0005"
        )).thenReturn(false);

        ConstraintViolationException databaseException =
                new ConstraintViolationException(
                        "Duplicate payment source reference",
                        null,
                        "uk_payment_source_reference"
                );

        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Database constraint violation",
                        databaseException
                ));

        assertThrows(
                PaymentAlreadyExistsException.class,
                () -> paymentService.createPayment(request)
        );

        verify(paymentRepository).saveAndFlush(any(Payment.class));
    }

    @Test
    void shouldPropagateOtherDatabaseConstraintViolations() {
        PaymentCreateDTO request = new PaymentCreateDTO(
                "DEMO_SYSTEM",
                "DEMO-TXN-0006",
                "BR-001",
                new BigDecimal("75.50"),
                "USD"
        );

        when(paymentRepository.existsBySourceSystemAndSourcePaymentReference(
                "DEMO_SYSTEM",
                "DEMO-TXN-0006"
        )).thenReturn(false);

        ConstraintViolationException databaseException =
                new ConstraintViolationException(
                        "Another database constraint was violated",
                        null,
                        "some_other_constraint"
                );

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Database constraint violation",
                        databaseException
                );

        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenThrow(exception);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> paymentService.createPayment(request)
        );

        assertSame(exception, thrown);
        verify(paymentRepository).saveAndFlush(any(Payment.class));
    }
    @Test
    void shouldReturnPaymentsForBranch() {

        Payment payment1 = mock(Payment.class);
        Payment payment2 = mock(Payment.class);

        when(payment1.getReference()).thenReturn("PAY-001");
        when(payment1.getSourceSystem()).thenReturn("DEMO_SYSTEM");
        when(payment1.getSourcePaymentReference()).thenReturn("TXN-001");
        when(payment1.getBranchCode()).thenReturn("BR-001");
        when(payment1.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(payment1.getCurrency()).thenReturn("USD");
        when(payment1.getStatus()).thenReturn(PaymentStatus.SUCCESS);

        when(payment2.getReference()).thenReturn("PAY-002");
        when(payment2.getSourceSystem()).thenReturn("DEMO_SYSTEM");
        when(payment2.getSourcePaymentReference()).thenReturn("TXN-002");
        when(payment2.getBranchCode()).thenReturn("BR-001");
        when(payment2.getAmount()).thenReturn(new BigDecimal("50.00"));
        when(payment2.getCurrency()).thenReturn("USD");
        when(payment2.getStatus()).thenReturn(PaymentStatus.PENDING);

        when(paymentRepository.findByBranchCode("BR-001"))
                .thenReturn(List.of(payment1, payment2));

        List<PaymentResponseDTO> result =
                paymentService.getPaymentsByBranch("BR-001");

        assertEquals(2, result.size());

        assertEquals("PAY-001", result.get(0).reference());
        assertEquals("SUCCESS", result.get(0).status());

        assertEquals("PAY-002", result.get(1).reference());
        assertEquals("PENDING", result.get(1).status());

        verify(paymentRepository).findByBranchCode("BR-001");
    }
    @Test
    void shouldMarkPaymentAsProcessing() {

        Payment payment = new Payment(
                "PAY-007",
                "DEMO_SYSTEM",
                "TXN-007",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        when(paymentRepository.findByReference("PAY-007"))
                .thenReturn(Optional.of(payment));

        PaymentResponseDTO response =
                paymentService.markAsProcessing(
                        payment.getReference()
                );

        assertEquals(
                PaymentStatus.PROCESSING,
                payment.getStatus()
        );

        assertEquals(
                "PROCESSING",
                response.status()
        );

        assertEquals(
                "PAY-007",
                response.reference()
        );

        verify(paymentAlertService)
                .resolveOpenAlerts(
                        payment.getReference()
                );
    }
    @Test
    void shouldNotResolveAlertsWhenMarkAsProcessingFails() {

        Payment payment = new Payment(
                "PAY-008",
                "DEMO_SYSTEM",
                "TXN-008",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        // Première transition valide :
        // PENDING -> PROCESSING
        payment.markAsProcessing(now);

        when(paymentRepository.findByReference("PAY-008"))
                .thenReturn(Optional.of(payment));

        // Deuxième transition interdite :
        // PROCESSING -> PROCESSING
        assertThrows(
                InvalidPaymentStatusTransitionException.class,
                () -> paymentService.markAsProcessing(
                        payment.getReference()
                )
        );

        // Comme la transition a échoué,
        // aucune alerte ne doit être résolue
        verify(paymentAlertService, never())
                .resolveOpenAlerts(anyString());
    }
    @Test
    void shouldThrowExceptionWhenMarkingUnknownPaymentAsProcessing() {

        when(paymentRepository.findByReference("PAY-UNKNOWN"))
                .thenReturn(Optional.empty());

        PaymentNotFoundException exception = assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.markAsProcessing("PAY-UNKNOWN")
        );

        assertEquals(
                "Payment not found with reference: PAY-UNKNOWN",
                exception.getMessage()
        );

        verify(paymentRepository).findByReference("PAY-UNKNOWN");
    }
    @Test
    void shouldMarkPaymentAsSuccess() {

        Payment payment = new Payment(
                "PAY-008",
                "DEMO_SYSTEM",
                "TXN-008",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );
        LocalDateTime now =
                LocalDateTime.of(2026, 10, 1, 12, 0);
        // Un paiement doit d'abord être PROCESSING
        payment.markAsProcessing(now);

        when(paymentRepository.findByReference("PAY-008"))
                .thenReturn(Optional.of(payment));

        PaymentResponseDTO response =
                paymentService.markAsSuccess("PAY-008");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("SUCCESS", response.status());
        assertEquals("PAY-008", response.reference());

        verify(paymentRepository).findByReference("PAY-008");
    }
    @Test
    void shouldThrowExceptionWhenMarkingPendingPaymentAsSuccess() {

        Payment payment = new Payment(
                "PAY-009",
                "DEMO_SYSTEM",
                "TXN-009",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        // Le paiement est encore PENDING ici

        when(paymentRepository.findByReference("PAY-009"))
                .thenReturn(Optional.of(payment));

        InvalidPaymentStatusTransitionException exception =
                assertThrows(
                        InvalidPaymentStatusTransitionException.class,
                        () -> paymentService.markAsSuccess("PAY-009")
                );

        assertEquals(
                "Cannot change payment status from PENDING to SUCCESS",
                exception.getMessage()
        );

        assertEquals(PaymentStatus.PENDING, payment.getStatus());

        verify(paymentRepository).findByReference("PAY-009");
    }
    @Test
    void shouldMarkPaymentAsFailed(){
        Payment payment = new Payment(
                "PAY-010",
                "DEMO_SYSTEM",
                "TXN-010",
                "BR-001",
                new BigDecimal("200.00"),
                "USD",
                now
        );
        LocalDateTime now =
                LocalDateTime.of(2026, 10, 1, 12, 0);
        payment.markAsProcessing(now);

        when(paymentRepository.findByReference("PAY-010"))
                .thenReturn(Optional.of(payment));

        PaymentResponseDTO response =
                paymentService.markAsFailed("PAY-010");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("FAILED", response.status());
        assertEquals("PAY-010", response.reference());

        verify(paymentRepository).findByReference("PAY-010");

    }
    @Test
    void shouldGetPaymentsByStatus() {

        Payment payment1 = new Payment(
                "PAY-001",
                "DEMO_SYSTEM",
                "TXN-001",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        Payment payment2 = new Payment(
                "PAY-002",
                "DEMO_SYSTEM",
                "TXN-002",
                "BR-001",
                new BigDecimal("50.00"),
                "USD",
                now
        );

        when(paymentRepository.findByStatus(PaymentStatus.PENDING))
                .thenReturn(List.of(payment1, payment2));

        List<PaymentResponseDTO> result =
                paymentService.getPaymentsByStatus(PaymentStatus.PENDING);

        assertEquals(2, result.size());

        assertEquals("PAY-001", result.get(0).reference());
        assertEquals("PENDING", result.get(0).status());

        assertEquals("PAY-002", result.get(1).reference());
        assertEquals("PENDING", result.get(1).status());

        verify(paymentRepository).findByStatus(PaymentStatus.PENDING);
    }
    @Test
    void shouldGetStalePendingPayments() {

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
                LocalDateTime.of(2026, 10, 1, 11, 55, 0);

        when(paymentRepository.findByStatusAndCreatedAtBefore(
                PaymentStatus.PENDING,
                expectedThreshold
        )).thenReturn(List.of(payment));

        List<PaymentResponseDTO> result =
                paymentService.getStalePendingPayments();

        assertEquals(1, result.size());
        assertEquals("PAY-011", result.get(0).reference());
        assertEquals("PENDING", result.get(0).status());

        verify(paymentRepository)
                .findByStatusAndCreatedAtBefore(
                        PaymentStatus.PENDING,
                        expectedThreshold
                );
    }
    @Test
    void shouldUseConfiguredPendingThreshold() {

        monitoringProperties.setPendingThresholdMinutes(10);

        LocalDateTime expectedThreshold =
                LocalDateTime.of(2026, 10, 1, 11, 50, 0);

        when(paymentRepository.findByStatusAndCreatedAtBefore(
                PaymentStatus.PENDING,
                expectedThreshold
        )).thenReturn(List.of());

        List<PaymentResponseDTO> result =
                paymentService.getStalePendingPayments();

        assertTrue(result.isEmpty());

        verify(paymentRepository)
                .findByStatusAndCreatedAtBefore(
                        PaymentStatus.PENDING,
                        expectedThreshold
                );
    }
    @Test
    void shouldReturnPaymentsByStatusForBranch() {

        Payment payment = new Payment(
                "PAY-2026-0021",
                "DEMO_SYSTEM",
                "DEMO-TXN-0021",
                "BR-001",
                new BigDecimal("150.00"),
                "USD",
                now
        );

        when(
                paymentRepository.findByStatusAndBranchCode(
                        PaymentStatus.PENDING,
                        "BR-001"
                )
        ).thenReturn(List.of(payment));

        List<PaymentResponseDTO> result =
                paymentService.getPaymentsByStatusAndBranch(
                        PaymentStatus.PENDING,
                        "BR-001"
                );

        assertEquals(1, result.size());
        assertEquals("PAY-2026-0021", result.get(0).reference());
        assertEquals("BR-001", result.get(0).branchCode());

        verify(paymentRepository)
                .findByStatusAndBranchCode(
                        PaymentStatus.PENDING,
                        "BR-001"
                );

        verify(paymentRepository, never())
                .findByStatus(PaymentStatus.PENDING);
    }
    @Test
    void shouldReturnStalePendingPaymentsForBranch() {

        LocalDateTime threshold =
                LocalDateTime.of(2026, 10, 1, 11, 55);

        Payment stalePayment = new Payment(
                "PAY-STALE-001",
                "DEMO_SYSTEM",
                "DEMO-TXN-STALE-001",
                "BR-001",
                new BigDecimal("200.00"),
                "USD",
                LocalDateTime.of(2026, 10, 1, 11, 50)
        );

        when(
                paymentRepository
                        .findByStatusAndBranchCodeAndCreatedAtBefore(
                                PaymentStatus.PENDING,
                                "BR-001",
                                threshold
                        )
        ).thenReturn(List.of(stalePayment));

        List<PaymentResponseDTO> result =
                paymentService.getStalePendingPaymentsForBranch(
                        "BR-001"
                );

        assertEquals(1, result.size());
        assertEquals("PAY-STALE-001", result.get(0).reference());
        assertEquals("BR-001", result.get(0).branchCode());
        assertEquals("PENDING", result.get(0).status());

        verify(paymentRepository)
                .findByStatusAndBranchCodeAndCreatedAtBefore(
                        PaymentStatus.PENDING,
                        "BR-001",
                        threshold
                );

        verify(paymentRepository, never())
                .findByStatusAndCreatedAtBefore(
                        any(PaymentStatus.class),
                        any(LocalDateTime.class)
                );
    }
}