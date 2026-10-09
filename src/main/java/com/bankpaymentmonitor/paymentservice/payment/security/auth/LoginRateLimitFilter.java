package com.bankpaymentmonitor.paymentservice.payment.security.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.util.function.LongSupplier;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_NANOS =
            Duration.ofMinutes(1).toNanos();

    private final ConcurrentHashMap<String, Counter> counters =
            new ConcurrentHashMap<>();

    private final LongSupplier nanoTimeSupplier;

    public LoginRateLimitFilter() {
        this(System::nanoTime);
    }

    LoginRateLimitFilter(LongSupplier nanoTimeSupplier) {
        this.nanoTimeSupplier = nanoTimeSupplier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/login".equals(path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        long now = nanoTimeSupplier.getAsLong();

        Counter counter = counters.compute(clientIp, (ip, current) -> {

            if (current == null
                    || now - current.windowStart() >= WINDOW_NANOS) {
                return new Counter(1, now);
            }

            return new Counter(
                    current.requests() + 1,
                    current.windowStart()
            );
        });

        if (counter.requests() > MAX_REQUESTS) {

            long remainingNanos =
                    WINDOW_NANOS - (now - counter.windowStart());

            long retryAfterSeconds = Math.max(
                    1,
                    (long) Math.ceil(
                            remainingNanos / 1_000_000_000.0
                    )
            );

            response.setStatus(429);
            response.setHeader(
                    "Retry-After",
                    String.valueOf(retryAfterSeconds)
            );
            response.setContentType("application/json");

            response.getWriter().write(
                    "{\"error\":\"TOO_MANY_REQUESTS\","
                            + "\"message\":\"Too many login attempts\"}"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
    int getCounterCount() {
        return counters.size();
    }
    @Scheduled(fixedDelay = 60_000)
    void cleanupExpiredCounters() {

        long now = nanoTimeSupplier.getAsLong();

        counters.forEach((ip, counter) -> {
            if (now - counter.windowStart() >= WINDOW_NANOS) {
                counters.remove(ip, counter);
            }
        });
    }

    private record Counter(int requests, long windowStart) {
    }

}