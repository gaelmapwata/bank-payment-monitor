package com.bankpaymentmonitor.paymentservice.payment.audit;

import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import com.bankpaymentmonitor.paymentservice.payment.security.AppUser;
import com.bankpaymentmonitor.paymentservice.payment.security.CustomUserPrincipal;
import com.bankpaymentmonitor.paymentservice.payment.security.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentAuditControllerForbiddenTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @Test
    void shouldReturn403WhenUserReadsAnotherBranchAudits()
            throws Exception {

        // GIVEN : utilisateur BR-001
        AppUser user = new AppUser(
                "john",
                "encoded-password",
                "BR-001",
                Role.USER,
                true
        );

        CustomUserPrincipal principal =
                new CustomUserPrincipal(user);

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        // Paiement appartenant à BR-002
        Payment payment = mock(Payment.class);

        when(payment.getBranchCode()).thenReturn("BR-002");

        when(paymentRepository.findByReference("PAY-002"))
                .thenReturn(Optional.of(payment));

        // WHEN / THEN
        mockMvc.perform(
                        get("/api/payments/PAY-002/audits")
                                .servletPath("/api")
                                .with(authentication(authentication))
                )
                .andExpect(status().isForbidden());
    }
    @Test
    void shouldReturn404WhenPaymentDoesNotExist() throws Exception {

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

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        String reference = "PAY-UNKNOWN";

        when(paymentRepository.findByReference(reference))
                .thenReturn(Optional.empty());

        // WHEN / THEN
        mockMvc.perform(
                        get("/api/payments/" + reference + "/audits")
                                .servletPath("/api")
                                .with(authentication(authentication))
                )
                .andExpect(status().isNotFound());

        verify(paymentRepository).findByReference(reference);
    }
    @Test
    void shouldReturn200WhenAdminReadsPaymentFromAnotherBranch()
            throws Exception {

        // GIVEN
        AppUser admin = new AppUser(
                "admin",
                "encoded-password",
                null,
                Role.ADMIN,
                true
        );

        CustomUserPrincipal principal =
                new CustomUserPrincipal(admin);

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        Payment payment = mock(Payment.class);

        when(paymentRepository.findByReference("PAY-003"))
                .thenReturn(Optional.of(payment));

        // WHEN / THEN
        mockMvc.perform(
                        get("/api/payments/PAY-003/audits")
                                .servletPath("/api")
                                .with(authentication(authentication))
                )
                .andExpect(status().isOk());

        verify(paymentRepository).findByReference("PAY-003");
    }
}