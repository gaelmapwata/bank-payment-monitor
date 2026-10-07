package com.bankpaymentmonitor.paymentservice.payment.service;

import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertService;
import com.bankpaymentmonitor.paymentservice.payment.audit.PaymentAuditService;
import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentCreateDTO;
import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.exception.PaymentAlreadyExistsException;
import com.bankpaymentmonitor.paymentservice.payment.exception.PaymentNotFoundException;
import com.bankpaymentmonitor.paymentservice.payment.mapper.PaymentMapper;
import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import com.bankpaymentmonitor.paymentservice.config.PaymentMonitoringProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentMonitoringProperties monitoringProperties;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentAlertService paymentAlertService;
    private final PaymentAuditService paymentAuditService;
    private final Clock clock;

    public PaymentService(
            PaymentMonitoringProperties monitoringProperties,
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            Clock clock,
            PaymentAlertService paymentAlertService,
            PaymentAuditService paymentAuditService
    ) {
        this.monitoringProperties = monitoringProperties;
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.clock = clock;
        this.paymentAlertService = paymentAlertService;
        this.paymentAuditService = paymentAuditService;

    }

    public PaymentResponseDTO getPaymentByReference(String reference) {
        Payment payment = findPaymentByReference(reference);

        return paymentMapper.toResponseDTO(payment);
    }
    @Transactional
    public PaymentResponseDTO createPayment(PaymentCreateDTO request) {

        boolean alreadyExists =
                paymentRepository.existsBySourceSystemAndSourcePaymentReference(
                        request.sourceSystem(),
                        request.sourcePaymentReference()
                );

        if (alreadyExists) {
            throw new PaymentAlreadyExistsException(
                    request.sourceSystem(),
                    request.sourcePaymentReference()
            );
        }

        String reference = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(clock);

        Payment payment = new Payment(
                reference,
                request.sourceSystem(),
                request.sourcePaymentReference(),
                request.branchCode(),
                request.amount(),
                request.currency(),
                now
        );

        Payment savedPayment;

        try {
            savedPayment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException exception) {

            Throwable cause = exception;

            while (cause != null) {
                if (cause instanceof ConstraintViolationException constraintException
                        && "uk_payment_source_reference".equals(
                        constraintException.getConstraintName())) {

                    throw new PaymentAlreadyExistsException(
                            request.sourceSystem(),
                            request.sourcePaymentReference()
                    );
                }

                cause = cause.getCause();
            }

            throw exception;
        }
        return paymentMapper.toResponseDTO(payment);
    }
    public List<PaymentResponseDTO> getPaymentsByBranch(String branchCode) {

        List<Payment> payments = paymentRepository.findByBranchCode(branchCode);

        return payments.stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }
    @Transactional
    public PaymentResponseDTO markAsProcessing(String reference) {

        Payment payment = findPaymentByReference(reference);
        PaymentStatus previousStatus = payment.getStatus();

        payment.markAsProcessing(
                LocalDateTime.now(clock)
        );

        paymentAlertService.resolveOpenAlerts(
                payment.getReference()
        );

        paymentAuditService.recordStatusChange(
                payment.getReference(),
                previousStatus,
                payment.getStatus()
        );

        return paymentMapper.toResponseDTO(payment);
    }
    @Transactional
    public PaymentResponseDTO markAsSuccess(String reference) {

        Payment payment = findPaymentByReference(reference);

        PaymentStatus previousStatus = payment.getStatus();

        payment.markAsSuccess(LocalDateTime.now(clock));

        paymentAuditService.recordStatusChange(
                payment.getReference(),
                previousStatus,
                payment.getStatus()
        );

        return paymentMapper.toResponseDTO(payment);
    }
    @Transactional
    public PaymentResponseDTO markAsFailed(String reference) {

        Payment payment = findPaymentByReference(reference);

        PaymentStatus previousStatus = payment.getStatus();

        payment.markAsFailed(LocalDateTime.now(clock));

        paymentAuditService.recordStatusChange(
                payment.getReference(),
                previousStatus,
                payment.getStatus()
        );

        return paymentMapper.toResponseDTO(payment);
    }
    public List<PaymentResponseDTO> getPaymentsByStatus(PaymentStatus status) {

        List<Payment> payments = paymentRepository.findByStatus(status);

        return payments.stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }
    private Payment findPaymentByReference(String reference) {
        return paymentRepository.findByReference(reference)
                .orElseThrow(() -> new PaymentNotFoundException(reference));
    }
    public List<PaymentResponseDTO> getStalePendingPayments() {

        LocalDateTime threshold =
                LocalDateTime.now(clock)
                        .minusMinutes(
                                monitoringProperties.getPendingThresholdMinutes()
                        );

        List<Payment> payments =
                paymentRepository.findByStatusAndCreatedAtBefore(
                        PaymentStatus.PENDING,
                        threshold
                );

        return payments.stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }
    public List<PaymentResponseDTO> getPaymentsByStatusAndBranch(
            PaymentStatus status,
            String branchCode
    ) {

        return paymentRepository
                .findByStatusAndBranchCode(status, branchCode)
                .stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }
    public List<PaymentResponseDTO> getStalePendingPaymentsForBranch(
            String branchCode
    ) {

        LocalDateTime threshold = LocalDateTime.now(clock)
                .minusMinutes(
                        monitoringProperties.getPendingThresholdMinutes()
                );

        return paymentRepository
                .findByStatusAndBranchCodeAndCreatedAtBefore(
                        PaymentStatus.PENDING,
                        branchCode,
                        threshold
                )
                .stream()
                .map(paymentMapper::toResponseDTO)
                .toList();
    }

}