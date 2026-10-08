package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.audit.dto.PaymentAuditResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import com.bankpaymentmonitor.paymentservice.payment.security.AppUser;
import com.bankpaymentmonitor.paymentservice.payment.security.CustomUserPrincipal;
import com.bankpaymentmonitor.paymentservice.payment.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentAuditControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentAuditQueryService paymentAuditQueryService;
    @Test
    void shouldReturn401WhenRequestHasNoJwt() throws Exception {

        mockMvc.perform(
                        get("/api/payments/PAY-001/audits")
                )
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldReturn200WhenAuthenticatedUserReadsOwnBranchAudits()
            throws Exception {

        // GIVEN
        AppUser user = new AppUser(
                "john",
                "encoded-password",
                "BR-001",
                Role.USER,
                true
        );

        CustomUserPrincipal principal =
                new CustomUserPrincipal(user);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        PaymentAuditResponseDTO audit = new PaymentAuditResponseDTO(
                1L,
                "PAY-001",
                PaymentAuditAction.STATUS_CHANGE,
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING,
                "john",
                "BR-001",
                LocalDateTime.of(2026, 10, 8, 12, 0)
        );

        when(paymentAuditQueryService
                .getAuditsByPaymentReference("PAY-001"))
                .thenReturn(List.of(audit));

        // WHEN / THEN
        mockMvc.perform(
                        get("/api/payments/PAY-001/audits")
                                .servletPath("/api")
                                .with(authentication(authentication))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentReference")
                        .value("PAY-001"))
                .andExpect(jsonPath("$[0].performedBy")
                        .value("john"))
                .andExpect(jsonPath("$[0].branchCode")
                        .value("BR-001"));
    }
}