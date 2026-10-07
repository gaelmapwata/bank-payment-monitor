package com.bankpaymentmonitor.paymentservice.payment.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("dev")
class DevUserInitializerProfileTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldLoadDevUserInitializerWhenDevProfileIsActive() {

        DevUserInitializer initializer =
                applicationContext.getBean(DevUserInitializer.class);

        assertNotNull(initializer);
    }

}