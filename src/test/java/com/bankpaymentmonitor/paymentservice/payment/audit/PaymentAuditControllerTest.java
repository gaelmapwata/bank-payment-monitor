package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.audit.dto.PaymentAuditResponseDTO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAuditControllerTest {

    @Mock
    private PaymentAuditQueryService paymentAuditQueryService;

    @InjectMocks
    private PaymentAuditController paymentAuditController;

    @Test
    void shouldReturnAuditsForPaymentReference() {

        // GIVEN
        String reference = "PAY-001";

        when(paymentAuditQueryService
                .getAuditsByPaymentReference(reference))
                .thenReturn(List.of());

        // WHEN
        List<PaymentAuditResponseDTO> result =
                paymentAuditController.getAudits(reference);

        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(paymentAuditQueryService)
                .getAuditsByPaymentReference(reference);
    }
}