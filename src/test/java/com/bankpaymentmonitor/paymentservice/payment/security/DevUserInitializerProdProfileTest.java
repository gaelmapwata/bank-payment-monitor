package com.bankpaymentmonitor.paymentservice.payment.security;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("prod")
class DevUserInitializerProdProfileTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldNotLoadDevUserInitializerWhenProdProfileIsActive() {

        Map<String, DevUserInitializer> beans =
                applicationContext.getBeansOfType(
                        DevUserInitializer.class
                );

        assertTrue(beans.isEmpty());
    }
}