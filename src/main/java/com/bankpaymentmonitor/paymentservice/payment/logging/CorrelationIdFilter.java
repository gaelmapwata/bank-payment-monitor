package com.bankpaymentmonitor.paymentservice.payment.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    public static final String CORRELATION_ID_MDC_KEY =
            "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = UUID.randomUUID().toString();
        long startTime = System.nanoTime();

        boolean completedNormally = false;

        try {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            response.setHeader(
                    CORRELATION_ID_HEADER,
                    correlationId
            );

            log.info(
                    "HTTP request started method={} path={}",
                    request.getMethod(),
                    request.getRequestURI()
            );

            filterChain.doFilter(request, response);

            completedNormally = true;

        } catch (IOException | ServletException | RuntimeException ex) {

            log.error(
                    "HTTP request failed method={} path={} exception={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    ex.getClass().getSimpleName(),
                    ex
            );

            throw ex;

        } finally {

            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startTime
            );
            if (completedNormally) {

                int status = response.getStatus();

                if (status >= 500) {

                    log.error(
                            "HTTP request completed method={} path={} status={} durationMs={}",
                            request.getMethod(),
                            request.getRequestURI(),
                            status,
                            durationMs
                    );

                } else if (status >= 400) {

                    log.warn(
                            "HTTP request completed method={} path={} status={} durationMs={}",
                            request.getMethod(),
                            request.getRequestURI(),
                            status,
                            durationMs
                    );

                } else {

                    log.info(
                            "HTTP request completed method={} path={} status={} durationMs={}",
                            request.getMethod(),
                            request.getRequestURI(),
                            status,
                            durationMs
                    );
                }

            } else {

                log.warn(
                        "HTTP request interrupted method={} path={} durationMs={} status=UNRESOLVED",
                        request.getMethod(),
                        request.getRequestURI(),
                        durationMs
                );
            }

            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}