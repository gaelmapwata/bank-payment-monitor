package com.bankpaymentmonitor.paymentservice.payment.alert.controller;

import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertService;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertStatus;
import com.bankpaymentmonitor.paymentservice.payment.alert.dto.PaymentAlertResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.security.AppUser;
import com.bankpaymentmonitor.paymentservice.payment.security.BranchAccessService;
import com.bankpaymentmonitor.paymentservice.payment.security.CustomUserPrincipal;
import com.bankpaymentmonitor.paymentservice.payment.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class PaymentAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentAlertService paymentAlertService;

    @MockitoBean
    private BranchAccessService branchAccessService;

    @Test
    void shouldGetAlertsByBranch() throws Exception {

        LocalDateTime detectedAt =
                LocalDateTime.of(2026, 10, 2, 10, 0);

        PaymentAlertResponseDTO alert =
                new PaymentAlertResponseDTO(
                        1L,
                        "PAY-001",
                        "BR-001",
                        "STALE_PENDING",
                        "OPEN",
                        detectedAt,
                        null
                );
        when(
                branchAccessService.canAccessBranch(
                        any(CustomUserPrincipal.class),
                        eq("BR-001")
                )
        ).thenReturn(true);

        when(paymentAlertService.getAlertsByBranch("BR-001"))
                .thenReturn(List.of(alert));

        mockMvc.perform(
                        get("/api/alerts/branch/BR-001")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].paymentReference").value("PAY-001"))
                .andExpect(jsonPath("$[0].branchCode").value("BR-001"))
                .andExpect(jsonPath("$[0].type").value("STALE_PENDING"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].resolvedAt").isEmpty());
    }
    @Test
    void shouldGetAlertsByBranchAndStatus() throws Exception {

        LocalDateTime detectedAt =
                LocalDateTime.of(2026, 10, 2, 10, 0);

        PaymentAlertResponseDTO alert =
                new PaymentAlertResponseDTO(
                        1L,
                        "PAY-001",
                        "BR-001",
                        "STALE_PENDING",
                        "OPEN",
                        detectedAt,
                        null
                );
        when(
                branchAccessService.canAccessBranch(
                        any(CustomUserPrincipal.class),
                        eq("BR-001")
                )
        ).thenReturn(true);

        when(
                paymentAlertService.getAlertsByBranchAndStatus(
                        "BR-001",
                        PaymentAlertStatus.OPEN
                )
        ).thenReturn(List.of(alert));

        mockMvc.perform(
                        get("/api/alerts/branch/BR-001/status/OPEN")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(
                        jsonPath("$[0].paymentReference")
                                .value("PAY-001")
                )
                .andExpect(
                        jsonPath("$[0].branchCode")
                                .value("BR-001")
                )
                .andExpect(
                        jsonPath("$[0].type")
                                .value("STALE_PENDING")
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("OPEN")
                )
                .andExpect(
                        jsonPath("$[0].resolvedAt")
                                .isEmpty()
                );
    }
    @Test
    void shouldGetAlertsByPaymentReference() throws Exception {

        LocalDateTime detectedAt =
                LocalDateTime.of(2026, 10, 5, 9, 0);

        PaymentAlertResponseDTO dto =
                new PaymentAlertResponseDTO(
                        1L,
                        "PAY-001",
                        "BR-001",
                        "STALE_PENDING",
                        "OPEN",
                        detectedAt,
                        null
                );

        when(
                paymentAlertService.getAlertsByPaymentReference("PAY-001")
        ).thenReturn(List.of(dto));

        when(
                branchAccessService.canAccessPaymentAlerts(
                        any(CustomUserPrincipal.class),
                        eq("PAY-001")
                )
        ).thenReturn(true);

        mockMvc.perform(
                        get("/api/alerts/payment/PAY-001")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].paymentReference").value("PAY-001"))
                .andExpect(jsonPath("$[0].branchCode").value("BR-001"))
                .andExpect(jsonPath("$[0].type").value("STALE_PENDING"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        verify(paymentAlertService)
                .getAlertsByPaymentReference("PAY-001");
    }
    @Test
    void shouldReturnForbiddenWhenUserAccessesAnotherBranch() throws Exception {

        AppUser user = new AppUser(
                "john",
                "hashed-password",
                "BR-001",
                Role.USER,
                true
        );

        CustomUserPrincipal principal =
                new CustomUserPrincipal(user);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );


        mockMvc.perform(
                        get("/api/alerts/branch/BR-002")
                                .servletPath("/api")
                                .with(authentication(authentication))
                )
                .andExpect(status().isForbidden());

        verify(paymentAlertService, never())
                .getAlertsByBranch("BR-002");
    }
    private CustomUserPrincipal createBranchUser() {

        AppUser user = new AppUser(
                "john",
                "hashed-password",
                "BR-001",
                Role.USER,
                true
        );

        return new CustomUserPrincipal(user);
    }
    private CustomUserPrincipal createAdminUser() {

        AppUser admin = new AppUser(
                "admin",
                "hashed-password",
                null,
                Role.ADMIN,
                true
        );

        return new CustomUserPrincipal(admin);
    }
    @Test
    void shouldAllowAdminToAccessAnyBranch() throws Exception {

        when(
                branchAccessService.canAccessBranch(
                        any(CustomUserPrincipal.class),
                        eq("BR-002")
                )
        ).thenReturn(true);

        when(paymentAlertService.getAlertsByBranch("BR-002"))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/alerts/branch/BR-002")
                                .servletPath("/api")
                                .with(user(createAdminUser()))
                )
                .andExpect(status().isOk());

        verify(paymentAlertService)
                .getAlertsByBranch("BR-002");
    }
    @Test
    void shouldForbidUserFromAccessingAnotherBranchAlertsByStatus() throws Exception {

        mockMvc.perform(
                        get("/api/alerts/branch/BR-002/status/OPEN")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isForbidden());

        verify(paymentAlertService, never())
                .getAlertsByBranchAndStatus(
                        "BR-002",
                        PaymentAlertStatus.OPEN
                );
    }
    @Test
    void shouldForbidUserFromAccessingAlertsOfPaymentFromAnotherBranch() throws Exception {

        mockMvc.perform(
                        get("/api/alerts/payment/PAY-BR002-001")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isForbidden());

        verify(paymentAlertService, never())
                .getAlertsByPaymentReference("PAY-BR002-001");
    }
}