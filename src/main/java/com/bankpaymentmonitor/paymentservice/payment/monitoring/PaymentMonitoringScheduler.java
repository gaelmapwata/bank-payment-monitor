package com.bankpaymentmonitor.paymentservice.payment.monitoring;

import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlert;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PaymentMonitoringScheduler {

    private final PaymentAlertService paymentAlertService;

    public PaymentMonitoringScheduler(
            PaymentAlertService paymentAlertService
    ) {
        this.paymentAlertService = paymentAlertService;
    }

    @Scheduled(
            fixedDelayString =
                    "${payment.monitoring.scan-interval-ms}"
    )
    public List<PaymentAlert> runMonitoring() {

        log.info("Starting stale pending payment monitoring");

        List<PaymentAlert> alerts =
                paymentAlertService.detectStalePendingAlerts();

        log.info(
                "Stale pending payment monitoring completed. New alerts created: {}",
                alerts.size()
        );

        return alerts;
    }
}