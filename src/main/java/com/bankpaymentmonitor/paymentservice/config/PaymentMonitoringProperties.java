package com.bankpaymentmonitor.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment.monitoring")
public class PaymentMonitoringProperties {

    private long pendingThresholdMinutes = 5;

    public long getPendingThresholdMinutes() {
        return pendingThresholdMinutes;
    }

    public void setPendingThresholdMinutes(long pendingThresholdMinutes) {
        this.pendingThresholdMinutes = pendingThresholdMinutes;
    }
}