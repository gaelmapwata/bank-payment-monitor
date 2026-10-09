package com.bankpaymentmonitor.paymentservice.payment.security.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginRateLimitFilterTest {

    @Test
    void shouldRemoveExpiredCounters() throws Exception {

        AtomicLong clock = new AtomicLong(1_000_000_000L);

        LoginRateLimitFilter filter =
                new LoginRateLimitFilter(clock::get);

        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/auth/login");

        request.setServletPath("/api");
        request.setRemoteAddr("192.0.2.30");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(request, response, mock(
                jakarta.servlet.FilterChain.class
        ));

        assertEquals(1, filter.getCounterCount());

        // Simuler 61 secondes sans attendre réellement
        clock.addAndGet(Duration.ofSeconds(61).toNanos());

        filter.cleanupExpiredCounters();

        assertEquals(0, filter.getCounterCount());
    }
}