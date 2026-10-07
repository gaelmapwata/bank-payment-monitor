package com.bankpaymentmonitor.paymentservice.payment.monitoring;

import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlert;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentMonitoringSchedulerTest {

    @Mock
    private PaymentAlertService paymentAlertService;

    private PaymentMonitoringScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        scheduler =
                new PaymentMonitoringScheduler(paymentAlertService);
    }

    @Test
    void shouldRunStalePendingDetection() {

        List<PaymentAlert> expectedAlerts = List.of();

        when(paymentAlertService.detectStalePendingAlerts())
                .thenReturn(expectedAlerts);

        List<PaymentAlert> result =
                scheduler.runMonitoring();

        assertEquals(expectedAlerts, result);

        verify(paymentAlertService)
                .detectStalePendingAlerts();
    }
}