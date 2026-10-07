package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentAuditServiceTest {

    @Test
    void shouldRecordStatusChangeAudit() {

        PaymentAuditRepository repository =
                Mockito.mock(PaymentAuditRepository.class);

        Clock clock = Clock.fixed(
                Instant.parse("2026-10-07T15:30:00Z"),
                ZoneOffset.UTC
        );

        PaymentAuditService service =
                new PaymentAuditService(repository, clock);


        service.recordStatusChange(
                "PAY-2026-001",
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING
        );

        ArgumentCaptor<PaymentAudit> auditCaptor =
                ArgumentCaptor.forClass(PaymentAudit.class);

        Mockito.verify(repository).save(auditCaptor.capture());

        PaymentAudit savedAudit = auditCaptor.getValue();

        assertEquals(
                "PAY-2026-001",
                savedAudit.getPaymentReference()
        );

        assertEquals(
                PaymentAuditAction.STATUS_CHANGE,
                savedAudit.getAction()
        );

        assertEquals(
                PaymentStatus.PENDING,
                savedAudit.getPreviousStatus()
        );

        assertEquals(
                PaymentStatus.PROCESSING,
                savedAudit.getNewStatus()
        );

        assertEquals(
                LocalDateTime.of(2026, 10, 7, 15, 30),
                savedAudit.getOccurredAt()
        );
    }
}