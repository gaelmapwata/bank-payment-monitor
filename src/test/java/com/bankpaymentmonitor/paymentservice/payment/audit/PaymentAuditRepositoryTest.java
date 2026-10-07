package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PaymentAuditRepositoryTest {

    @Autowired
    private PaymentAuditRepository paymentAuditRepository;

    @Test
    void shouldSavePaymentAudit() {

        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 10, 7, 17, 30);

        PaymentAudit audit = new PaymentAudit(
                "PAY-AUDIT-001",
                PaymentAuditAction.STATUS_CHANGE,
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING,
                occurredAt
        );

        PaymentAudit savedAudit =
                paymentAuditRepository.saveAndFlush(audit);

        assertNotNull(savedAudit.getId());
    }
    @Test
    void shouldSaveAndRetrievePaymentAudit() {

        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 10, 7, 17, 30);

        PaymentAudit audit = new PaymentAudit(
                "PAY-AUDIT-002",
                PaymentAuditAction.STATUS_CHANGE,
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING,
                occurredAt
        );

        PaymentAudit savedAudit =
                paymentAuditRepository.saveAndFlush(audit);

        PaymentAudit foundAudit =
                paymentAuditRepository.findById(savedAudit.getId())
                        .orElseThrow();

        assertEquals(
                "PAY-AUDIT-002",
                foundAudit.getPaymentReference()
        );

        assertEquals(
                PaymentAuditAction.STATUS_CHANGE,
                foundAudit.getAction()
        );

        assertEquals(
                PaymentStatus.PENDING,
                foundAudit.getPreviousStatus()
        );

        assertEquals(
                PaymentStatus.PROCESSING,
                foundAudit.getNewStatus()
        );

        assertEquals(
                occurredAt,
                foundAudit.getOccurredAt()
        );
    }
    @Test
    void shouldFindAuditsByPaymentReferenceOrderedByOccurredAt() {

        PaymentAudit firstAudit = new PaymentAudit(
                "PAY-HISTORY-001",
                PaymentAuditAction.STATUS_CHANGE,
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING,
                LocalDateTime.of(2026, 10, 7, 10, 0)
        );

        PaymentAudit secondAudit = new PaymentAudit(
                "PAY-HISTORY-001",
                PaymentAuditAction.STATUS_CHANGE,
                PaymentStatus.PROCESSING,
                PaymentStatus.SUCCESS,
                LocalDateTime.of(2026, 10, 7, 10, 5)
        );

        paymentAuditRepository.saveAllAndFlush(
                List.of(secondAudit, firstAudit)
        );

        List<PaymentAudit> audits =
                paymentAuditRepository
                        .findByPaymentReferenceOrderByOccurredAtAsc(
                                "PAY-HISTORY-001"
                        );

        assertEquals(2, audits.size());

        assertEquals(
                PaymentStatus.PENDING,
                audits.get(0).getPreviousStatus()
        );

        assertEquals(
                PaymentStatus.PROCESSING,
                audits.get(0).getNewStatus()
        );

        assertEquals(
                PaymentStatus.PROCESSING,
                audits.get(1).getPreviousStatus()
        );

        assertEquals(
                PaymentStatus.SUCCESS,
                audits.get(1).getNewStatus()
        );
    }
}