package com.bankpaymentmonitor.paymentservice.payment.controller;

import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentCreateDTO;
import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentResponseDTO;
import com.bankpaymentmonitor.paymentservice.payment.security.AppUser;
import com.bankpaymentmonitor.paymentservice.payment.security.BranchAccessService;
import com.bankpaymentmonitor.paymentservice.payment.security.CustomUserPrincipal;
import com.bankpaymentmonitor.paymentservice.payment.security.Role;
import com.bankpaymentmonitor.paymentservice.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.bankpaymentmonitor.paymentservice.payment.exception.PaymentNotFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.bankpaymentmonitor.paymentservice.payment.enums.PaymentStatus;
import static org.mockito.Mockito.verifyNoInteractions;
import com.bankpaymentmonitor.paymentservice.payment.exception.InvalidPaymentStatusTransitionException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import com.bankpaymentmonitor.paymentservice.payment.security.BranchAccessService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private BranchAccessService branchAccessService;

    @Test
    void shouldReturnPaymentWhenReferenceExists() throws Exception  {

        PaymentResponseDTO response = new PaymentResponseDTO(
                "PAY-2026-0001",
                "DEMO_SYSTEM",
                "DEMO-TXN-0001",
                "BR-001",
                new BigDecimal("150.00"),
                "USD",
                "SUCCESS"
        );

        when(paymentService.getPaymentByReference("PAY-2026-0001"))
                .thenReturn(response);
        when(
                branchAccessService.canAccessPayment(
                        any(CustomUserPrincipal.class),
                        eq("PAY-2026-0001")
                )
        ).thenReturn(true);

        mockMvc.perform(
                        get("/api/payments/PAY-2026-0001")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("PAY-2026-0001"))
                .andExpect(jsonPath("$.sourceSystem").value("DEMO_SYSTEM"))
                .andExpect(jsonPath("$.sourcePaymentReference").value("DEMO-TXN-0001"))
                .andExpect(jsonPath("$.branchCode").value("BR-001"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void shouldReturn404WhenPaymentDoesNotExist() throws Exception {

        when(
                branchAccessService.canAccessPayment(
                        any(CustomUserPrincipal.class),
                        eq("PAY-2026-0002")
                )
        ).thenReturn(true);

        when(paymentService.getPaymentByReference("PAY-2026-0002"))
                .thenThrow(
                        new PaymentNotFoundException("PAY-2026-0002")
                );

        mockMvc.perform(
                        get("/api/payments/PAY-2026-0002")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("PAYMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Payment not found with reference: PAY-2026-0002"));
    }

    @Test
    void shouldReturnBadRequestWhenAmountIsNegative() throws Exception {

        String requestBody = """
            {
              "sourceSystem": "DEMO_SYSTEM",
              "sourcePaymentReference": "DEMO-TXN-INVALID-001",
              "branchCode": "BR-001",
              "amount": -75.50,
              "currency": "USD"
            }
            """;

        mockMvc.perform(post("/api/payments")
                        .servletPath("/api")
                        .with(user(createBranchUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.amount")
                        .value("Le montant doit être supérieur à zéro"));
    }

    @Test
    void shouldReturnBadRequestWhenSourceSystemIsBlank() throws Exception {

        String requestBody = """
            {
              "sourceSystem": "",
              "sourcePaymentReference": "DEMO-TXN-INVALID-002",
              "branchCode": "BR-001",
              "amount": 75.50,
              "currency": "USD"
            }
            """;

        mockMvc.perform(post("/api/payments")
                        .servletPath("/api")
                        .with(user(createBranchUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.sourceSystem")
                        .value("Le système source est obligatoire"));
    }

    @Test
    void shouldReturnPaymentsForBranch() throws Exception {

        PaymentResponseDTO payment1 = new PaymentResponseDTO(
                "PAY-001",
                "DEMO_SYSTEM",
                "TXN-001",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                "SUCCESS"
        );

        PaymentResponseDTO payment2 = new PaymentResponseDTO(
                "PAY-002",
                "DEMO_SYSTEM",
                "TXN-002",
                "BR-001",
                new BigDecimal("50.00"),
                "USD",
                "PENDING"
        );

        when(paymentService.getPaymentsByBranch("BR-001"))
                .thenReturn(List.of(payment1, payment2));
        when(
                branchAccessService.canAccessBranch(
                        any(CustomUserPrincipal.class),
                        eq("BR-001")
                )
        ).thenReturn(true);

        mockMvc.perform(get("/api/payments/branch/BR-001")
                        .servletPath("/api")
                        .with(user(createBranchUser()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].reference").value("PAY-001"))
                .andExpect(jsonPath("$[0].branchCode").value("BR-001"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[1].reference").value("PAY-002"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));

        verify(paymentService).getPaymentsByBranch("BR-001");
    }
    @Test
    void shouldReturnConflictWhenPaymentStatusTransitionIsInvalid() throws Exception {

        String reference = "PAY-2026-0001";

        when(paymentService.markAsProcessing(reference))
                .thenThrow(
                        new InvalidPaymentStatusTransitionException(
                                PaymentStatus.PROCESSING,
                                PaymentStatus.PROCESSING
                        )
                );
        when(
                branchAccessService.canAccessPayment(
                        any(CustomUserPrincipal.class),
                        eq("PAY-2026-0001")
                )
        ).thenReturn(true);

        mockMvc.perform(
                        patch("/api/payments/{reference}/processing", reference)
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("INVALID_PAYMENT_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Cannot change payment status from PROCESSING to PROCESSING"
                        ));
    }
    @Test
    void shouldMarkPaymentAsProcessing() throws Exception {

        String reference = "PAY-2026-0001";

        PaymentResponseDTO response = new PaymentResponseDTO(
                reference,
                "DEMO_SYSTEM",
                "DEMO-TXN-0001",
                "BR-001",
                new BigDecimal("150.00"),
                "USD",
                "PROCESSING"
        );

        when(paymentService.markAsProcessing(reference))
                .thenReturn(response);
        when(
                branchAccessService.canAccessPayment(
                        any(CustomUserPrincipal.class),
                        eq("PAY-2026-0001")
                )
        ).thenReturn(true);

        mockMvc.perform(
                        patch("/api/payments/{reference}/processing", reference)
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.sourceSystem").value("DEMO_SYSTEM"))
                .andExpect(jsonPath("$.sourcePaymentReference").value("DEMO-TXN-0001"))
                .andExpect(jsonPath("$.branchCode").value("BR-001"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        verify(paymentService).markAsProcessing(reference);
    }
    @Test
    void shouldMarkPaymentAsSuccess() throws Exception {

        String reference = "PAY-2026-0001";

        PaymentResponseDTO response = new PaymentResponseDTO(
                reference,
                "DEMO_SYSTEM",
                "DEMO-TXN-0001",
                "BR-001",
                new BigDecimal("150.00"),
                "USD",
                "SUCCESS"
        );

        when(paymentService.markAsSuccess(reference))
                .thenReturn(response);
        when(
                branchAccessService.canAccessPayment(
                        any(CustomUserPrincipal.class),
                        eq("PAY-2026-0001")
                )
        ).thenReturn(true);

        mockMvc.perform(
                        patch("/api/payments/{reference}/success", reference)
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.sourceSystem").value("DEMO_SYSTEM"))
                .andExpect(jsonPath("$.sourcePaymentReference").value("DEMO-TXN-0001"))
                .andExpect(jsonPath("$.branchCode").value("BR-001"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentService).markAsSuccess(reference);
    }
    @Test
    void shouldReturnConflictWhenPendingPaymentIsMarkedAsSuccess() throws Exception {

        String reference = "PAY-2026-0001";

        when(paymentService.markAsSuccess(reference))
                .thenThrow(
                        new InvalidPaymentStatusTransitionException(
                                PaymentStatus.PENDING,
                                PaymentStatus.SUCCESS
                        )
                );
        when(
                branchAccessService.canAccessPayment(
                        any(CustomUserPrincipal.class),
                        eq("PAY-2026-0001")
                )
        ).thenReturn(true);

        mockMvc.perform(
                        patch("/api/payments/{reference}/success", reference)
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("INVALID_PAYMENT_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Cannot change payment status from PENDING to SUCCESS"
                        ));

        verify(paymentService).markAsSuccess(reference);
    }
    @Test
    void shouldMarkPaymentAsFailed()throws  Exception{
        String reference = "PAY-2026-0010";

        PaymentResponseDTO response = new PaymentResponseDTO(
                reference,
                "DEMO_SYSTEM",
                "DEMO-TXN-0010",
                "BR-001",
                new BigDecimal("150.00"),
                "USD",
                "FAILED"
        );

        when(paymentService.markAsFailed(reference))
                .thenReturn(response);
        when(
                branchAccessService.canAccessPayment(
                        any(CustomUserPrincipal.class),
                        eq(reference)
                )
        ).thenReturn(true);
        mockMvc.perform(
                        patch("/api/payments/{reference}/failed", reference)
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.sourceSystem").value("DEMO_SYSTEM"))
                .andExpect(jsonPath("$.sourcePaymentReference").value("DEMO-TXN-0010"))
                .andExpect(jsonPath("$.branchCode").value("BR-001"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("FAILED"));

        verify(paymentService).markAsFailed(reference);

    }
    @Test
    void shouldGetAllPaymentsByStatusWhenUserIsAdmin()throws Exception{
        PaymentResponseDTO payment1 = new PaymentResponseDTO(
                "PAY-001",
                "DEMO_SYSTEM",
                "TXN-001",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                "PENDING"
        );

        PaymentResponseDTO payment2 = new PaymentResponseDTO(
                "PAY-002",
                "DEMO_SYSTEM",
                "TXN-002",
                "BR-001",
                new BigDecimal("50.00"),
                "USD",
                "PENDING"
        );
        when(paymentService.getPaymentsByStatus(PaymentStatus.PENDING))
                .thenReturn(List.of(payment1, payment2));

        mockMvc.perform(get("/api/payments/status/PENDING")
                        .servletPath("/api")
                        .with(user(createAdminUser()))
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].reference").value("PAY-001"))
                .andExpect(jsonPath("$[0].branchCode").value("BR-001"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].reference").value("PAY-002"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));

        verify(paymentService).getPaymentsByStatus(PaymentStatus.PENDING);
    }
    @Test
    void shouldReturnBadRequestWhenPaymentStatusIsInvalid() throws Exception {

        mockMvc.perform(
                        get("/api/payments/status/WAITING")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("INVALID_PAYMENT_STATUS"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid payment status: WAITING"));

        verifyNoInteractions(paymentService);
    }
    @Test
    void shouldGetAllStalePendingPaymentsWhenUserIsAdmin() throws Exception{
        PaymentResponseDTO payment1 = new PaymentResponseDTO(
                "PAY-011",
                "DEMO_SYSTEM",
                "TXN-011",
                "BR-001",
                new BigDecimal("100.00"),
                "USD",
                "PENDING"
        );
        PaymentResponseDTO payment2 = new PaymentResponseDTO(
                "PAY-012",
                "DEMO_SYSTEM",
                "TXN-012",
                "BR-001",
                new BigDecimal("50.00"),
                "USD",
                "PENDING"
        );
        when(paymentService.getStalePendingPayments())
                .thenReturn(List.of(payment1, payment2));

        mockMvc.perform(get("/api/payments/pending/stale")
                        .servletPath("/api")
                        .with(user(createAdminUser()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].reference").value("PAY-011"))
                .andExpect(jsonPath("$[0].branchCode").value("BR-001"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].reference").value("PAY-012"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));

        verify(paymentService).getStalePendingPayments();
    }
    @Test
    void shouldForbidUserFromAccessingPaymentsOfAnotherBranch() throws Exception {

        mockMvc.perform(
                        get("/api/payments/branch/BR-002")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isForbidden());

        verify(paymentService, never())
                .getPaymentsByBranch("BR-002");
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
    @Test
    void shouldForbidUserFromAccessingPaymentOfAnotherBranch() throws Exception {

        mockMvc.perform(
                        get("/api/payments/PAY-BR002-001")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isForbidden());

        verify(paymentService, never())
                .getPaymentByReference("PAY-BR002-001");
    }
    @Test
    void shouldForbidUserFromMarkingAnotherBranchPaymentAsProcessing()
            throws Exception {

        when(
                branchAccessService.canAccessPayment(
                        any(CustomUserPrincipal.class),
                        eq("PAY-BR002-001")
                )
        ).thenReturn(false);

        mockMvc.perform(
                        patch("/api/payments/PAY-BR002-001/processing")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isForbidden());

        verify(paymentService, never())
                .markAsProcessing("PAY-BR002-001");
    }
    @Test
    void shouldForbidUserFromMarkingAnotherBranchPaymentAsSuccess()
            throws Exception {

        when(
                branchAccessService.canAccessPayment(
                        any(CustomUserPrincipal.class),
                        eq("PAY-BR002-001")
                )
        ).thenReturn(false);

        mockMvc.perform(
                        patch("/api/payments/PAY-BR002-001/success")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isForbidden());

        verify(paymentService, never())
                .markAsSuccess("PAY-BR002-001");
    }
    @Test
    void shouldForbidUserFromMarkingAnotherBranchPaymentAsFailed()
            throws Exception {

        when(
                branchAccessService.canAccessPayment(
                        any(CustomUserPrincipal.class),
                        eq("PAY-BR002-001")
                )
        ).thenReturn(false);

        mockMvc.perform(
                        patch("/api/payments/PAY-BR002-001/failed")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isForbidden());

        verify(paymentService, never())
                .markAsFailed("PAY-BR002-001");
    }
    @Test
    void shouldForbidUserFromCreatingPaymentForAnotherBranch()
            throws Exception {

        when(
                branchAccessService.canAccessBranch(
                        any(CustomUserPrincipal.class),
                        eq("BR-002")
                )
        ).thenReturn(false);

        String requestBody = """
        {
            "sourceSystem": "DEMO_SYSTEM",
            "sourcePaymentReference": "DEMO-TXN-BR002-001",
            "branchCode": "BR-002",
            "amount": 150.00,
            "currency": "USD"
        }
        """;

        mockMvc.perform(
                        post("/api/payments")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"));

        verify(paymentService, never())
                .createPayment(any(PaymentCreateDTO.class));
    }
    @Test
    void shouldAllowUserToCreatePaymentForOwnBranch() throws Exception {

        String reference = "PAY-2026-0020";

        PaymentResponseDTO response = new PaymentResponseDTO(
                reference,
                "DEMO_SYSTEM",
                "DEMO-TXN-0020",
                "BR-001",
                new BigDecimal("150.00"),
                "USD",
                "PENDING"
        );

        when(
                branchAccessService.canAccessBranch(
                        any(CustomUserPrincipal.class),
                        eq("BR-001")
                )
        ).thenReturn(true);

        when(paymentService.createPayment(any(PaymentCreateDTO.class)))
                .thenReturn(response);

        String requestBody = """
        {
            "sourceSystem": "DEMO_SYSTEM",
            "sourcePaymentReference": "DEMO-TXN-0020",
            "branchCode": "BR-001",
            "amount": 150.00,
            "currency": "USD"
        }
        """;

        mockMvc.perform(
                        post("/api/payments")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.sourceSystem").value("DEMO_SYSTEM"))
                .andExpect(jsonPath("$.sourcePaymentReference").value("DEMO-TXN-0020"))
                .andExpect(jsonPath("$.branchCode").value("BR-001"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentService)
                .createPayment(any(PaymentCreateDTO.class));
    }
    @Test
    void shouldReturnOnlyPaymentsFromUserBranchWhenGettingByStatus()
            throws Exception {

        PaymentResponseDTO response = new PaymentResponseDTO(
                "PAY-2026-0021",
                "DEMO_SYSTEM",
                "DEMO-TXN-0021",
                "BR-001",
                new BigDecimal("150.00"),
                "USD",
                "PENDING"
        );

        when(
                paymentService.getPaymentsByStatusAndBranch(
                        PaymentStatus.PENDING,
                        "BR-001"
                )
        ).thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/payments/status/PENDING")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reference")
                        .value("PAY-2026-0021"))
                .andExpect(jsonPath("$[0].branchCode")
                        .value("BR-001"))
                .andExpect(jsonPath("$[0].status")
                        .value("PENDING"));

        verify(paymentService)
                .getPaymentsByStatusAndBranch(
                        PaymentStatus.PENDING,
                        "BR-001"
                );

        verify(paymentService, never())
                .getPaymentsByStatus(PaymentStatus.PENDING);
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
    void shouldReturnOnlyStalePendingPaymentsFromUserBranch()
            throws Exception {

        PaymentResponseDTO response = new PaymentResponseDTO(
                "PAY-STALE-001",
                "DEMO_SYSTEM",
                "DEMO-TXN-STALE-001",
                "BR-001",
                new BigDecimal("200.00"),
                "USD",
                "PENDING"
        );

        when(
                paymentService.getStalePendingPaymentsForBranch("BR-001")
        ).thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/payments/pending/stale")
                                .servletPath("/api")
                                .with(user(createBranchUser()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reference")
                        .value("PAY-STALE-001"))
                .andExpect(jsonPath("$[0].branchCode")
                        .value("BR-001"))
                .andExpect(jsonPath("$[0].status")
                        .value("PENDING"));

        verify(paymentService)
                .getStalePendingPaymentsForBranch("BR-001");

        verify(paymentService, never())
                .getStalePendingPayments();
    }
    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(
                        get("/api/payments/status/PENDING")
                                .servletPath("/api")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));

        verifyNoInteractions(paymentService);
    }
}