package com.bankpaymentmonitor.paymentservice.payment.alert;

import com.bankpaymentmonitor.paymentservice.config.PaymentMonitoringProperties;
import com.bankpaymentmonitor.paymentservice.payment.alert.dto.PaymentAlertResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.alert.mapper.PaymentAlertMapper;
import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class PaymentAlertService {

    private final PaymentRepository paymentRepository;
    private final PaymentMonitoringProperties monitoringProperties;
    private final PaymentAlertRepository paymentAlertRepository;
    private final PaymentAlertMapper paymentAlertMapper;
    private final Clock clock;

    public PaymentAlertService(
            PaymentRepository paymentRepository,
            PaymentAlertRepository paymentAlertRepository,
            PaymentMonitoringProperties monitoringProperties,
            Clock clock,
            PaymentAlertMapper paymentAlertMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAlertRepository = paymentAlertRepository;
        this.monitoringProperties = monitoringProperties;
        this.clock = clock;
        this.paymentAlertMapper = paymentAlertMapper;
    }

    public PaymentAlert createStalePendingAlert(Payment payment) {

        return new PaymentAlert(
                payment.getReference(),
                payment.getBranchCode(),
                PaymentAlertType.STALE_PENDING,
                LocalDateTime.now(clock)
        );
    }
    public List<PaymentAlert> detectStalePendingAlerts() {

        LocalDateTime threshold =
                LocalDateTime.now(clock)
                        .minusMinutes(
                                monitoringProperties.getPendingThresholdMinutes()
                        );

        List<Payment> stalePayments =
                paymentRepository.findByStatusAndCreatedAtBefore(
                        PaymentStatus.PENDING,
                        threshold
                );

        return stalePayments.stream()
                .map(this::createAlertIfNotExists)
                .filter(Objects::nonNull)
                .toList();
    }
    private PaymentAlert createAlertIfNotExists(Payment payment) {

        boolean alreadyExists =
                paymentAlertRepository.existsByPaymentReferenceAndType(
                        payment.getReference(),
                        PaymentAlertType.STALE_PENDING
                );

        if (alreadyExists) {
            return null;
        }

        PaymentAlert alert = createStalePendingAlert(payment);

        try {
            return paymentAlertRepository.saveAndFlush(alert);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateAlertConstraint(exception)) {
                return null;
            }

            throw exception;
        }
    }
    private boolean isDuplicateAlertConstraint(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {

            if (cause instanceof
                    org.hibernate.exception.ConstraintViolationException
                            constraintViolationException) {

                return "uk_payment_alert".equals(
                        constraintViolationException.getConstraintName()
                );
            }

            cause = cause.getCause();
        }

        return false;
    }
    @Transactional
    public List<PaymentAlert> resolveOpenAlerts(
            String paymentReference
    ) {

        List<PaymentAlert> openAlerts =
                paymentAlertRepository.findByPaymentReferenceAndStatus(
                        paymentReference,
                        PaymentAlertStatus.OPEN
                );

        LocalDateTime now = LocalDateTime.now(clock);

        openAlerts.forEach(alert -> alert.resolve(now));

        return openAlerts;
    }

    @Transactional(readOnly = true)
    public List<PaymentAlertResponseDTO> getAlertsByBranch(
            String branchCode
    ) {
        return paymentAlertRepository
                .findByBranchCode(branchCode)
                .stream()
                .map(paymentAlertMapper::toResponseDTO)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<PaymentAlertResponseDTO> getAlertsByBranchAndStatus(
            String branchCode,
            PaymentAlertStatus status
    ) {
        return paymentAlertRepository
                .findByBranchCodeAndStatus(
                        branchCode,
                        status
                )
                .stream()
                .map(paymentAlertMapper::toResponseDTO)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<PaymentAlertResponseDTO> getAlertsByPaymentReference(
            String paymentReference
    ) {
        return paymentAlertRepository
                .findByPaymentReference(paymentReference)
                .stream()
                .map(paymentAlertMapper::toResponseDTO)
                .toList();
    }
}