package com.bankpaymentmonitor.paymentservice.payment.mapper;

import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponseDTO toResponseDTO(Payment payment) {

        return new PaymentResponseDTO(
                payment.getReference(),
                payment.getSourceSystem(),
                payment.getSourcePaymentReference(),
                payment.getBranchCode(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name()
        );
    }
}