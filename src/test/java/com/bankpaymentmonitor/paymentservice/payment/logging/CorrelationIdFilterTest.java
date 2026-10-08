package com.bankpaymentmonitor.paymentservice.payment.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import org.slf4j.MDC;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;

import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter =
            new CorrelationIdFilter();

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing()
            throws Exception {

        // GIVEN
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        // WHEN
        filter.doFilter(request, response, filterChain);

        // THEN
        String correlationId =
                response.getHeader("X-Correlation-ID");

        assertNotNull(correlationId);
        assertFalse(correlationId.isBlank());
        assertDoesNotThrow(() ->
                java.util.UUID.fromString(correlationId)
        );
    }
    @Test
    void shouldStoreCorrelationIdInMdcDuringRequest()
            throws Exception {

        // GIVEN
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        // Vérifier le MDC pendant l'exécution de la requête
        doAnswer(invocation -> {

            String correlationIdFromMdc =
                    MDC.get("correlationId");

            String correlationIdFromResponse =
                    response.getHeader("X-Correlation-ID");

            assertNotNull(correlationIdFromMdc);

            assertEquals(
                    correlationIdFromResponse,
                    correlationIdFromMdc
            );

            return null;

        }).when(filterChain).doFilter(request, response);

        // WHEN
        filter.doFilter(request, response, filterChain);

        // THEN
        verify(filterChain).doFilter(request, response);
    }
    @Test
    void shouldClearCorrelationIdFromMdcAfterRequest()
            throws Exception {

        // GIVEN
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        // WHEN
        filter.doFilter(request, response, filterChain);

        // THEN
        assertNull(MDC.get("correlationId"));

        verify(filterChain).doFilter(request, response);
    }
    @Test
    void shouldClearCorrelationIdFromMdcWhenExceptionOccurs()
            throws Exception {

        // GIVEN
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        doAnswer(invocation -> {

            // Le correlationId doit exister pendant le traitement
            assertNotNull(MDC.get("correlationId"));

            // Simulation d'une erreur technique
            throw new ServletException("Simulated processing failure");

        }).when(filterChain).doFilter(request, response);

        // WHEN / THEN
        assertThrows(
                ServletException.class,
                () -> filter.doFilter(request, response, filterChain)
        );

        // Même après l'exception, le MDC doit être nettoyé
        assertNull(MDC.get("correlationId"));

        verify(filterChain).doFilter(request, response);
    }
    @Test
    void shouldLogHttpRequestWithCorrelationId() throws Exception {

        // GIVEN
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/payments");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        // Capture des événements de logging
        Logger logger = (Logger) LoggerFactory.getLogger(
                CorrelationIdFilter.class
        );

        ListAppender<ILoggingEvent> listAppender =
                new ListAppender<>();

        listAppender.start();
        logger.addAppender(listAppender);

        try {
            // WHEN
            filter.doFilter(request, response, filterChain);

            // THEN
            List<ILoggingEvent> events = listAppender.list;

            assertTrue(events.stream().anyMatch(event ->
                    event.getFormattedMessage()
                            .contains("HTTP request started")
            ));

            assertTrue(events.stream().anyMatch(event ->
                    event.getFormattedMessage()
                            .contains("HTTP request completed")
            ));

            assertTrue(events.stream().allMatch(event ->
                    event.getMDCPropertyMap()
                            .containsKey("correlationId")
            ));

        } finally {
            logger.detachAppender(listAppender);
            listAppender.stop();
        }
    }
    @Test
    void shouldNotLogSuccessStatusWhenRequestThrowsException() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/payments");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        doAnswer(invocation -> {
            throw new ServletException("Simulated failure");
        }).when(filterChain).doFilter(request, response);

        Logger logger = (Logger) LoggerFactory.getLogger(
                CorrelationIdFilter.class
        );

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThrows(
                    ServletException.class,
                    () -> filter.doFilter(request, response, filterChain)
            );

            assertTrue(appender.list.stream()
                    .filter(event -> event.getFormattedMessage()
                            .contains("HTTP request completed"))
                    .noneMatch(event -> event.getFormattedMessage()
                            .contains("status=200")));

        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
    @Test
    void shouldLogForbiddenHttpResponseAsWarning() throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/payments");

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        doAnswer(invocation -> {
            response.setStatus(403);
            return null;
        }).when(filterChain).doFilter(request, response);

        Logger logger = (Logger) LoggerFactory.getLogger(
                CorrelationIdFilter.class
        );

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            filter.doFilter(request, response, filterChain);

            assertTrue(appender.list.stream()
                    .anyMatch(event ->
                            event.getLevel() == Level.WARN
                                    && event.getFormattedMessage()
                                    .contains("status=403")
                    ));

        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}