package com.bankpaymentmonitor.paymentservice.payment.security.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class LoginRateLimitSchedulingTest {

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    @Test
    void shouldRegisterAutomaticCounterCleanup() {

        boolean cleanupScheduled = scheduledTaskHolder
                .getScheduledTasks()
                .stream()
                .anyMatch(task ->
                        task.getTask()
                                .toString()
                                .contains("cleanupExpiredCounters")
                );

        assertTrue(
                cleanupScheduled,
                "Login rate limit cleanup must be scheduled"
        );
    }
}