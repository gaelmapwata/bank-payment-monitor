package com.bankpaymentmonitor.paymentservice.payment.alert.mapper;

import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlert;
import com.bankpaymentmonitor.paymentservice.payment.alert.dto.PaymentAlertResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class PaymentAlertMapper {

    public PaymentAlertResponseDTO toResponseDTO(
            PaymentAlert alert
    ) {
        return new PaymentAlertResponseDTO(
                alert.getId(),
                alert.getPaymentReference(),
                alert.getBranchCode(),
                alert.getType().name(),
                alert.getStatus().name(),
                alert.getDetectedAt(),
                alert.getResolvedAt()
        );
    }
}