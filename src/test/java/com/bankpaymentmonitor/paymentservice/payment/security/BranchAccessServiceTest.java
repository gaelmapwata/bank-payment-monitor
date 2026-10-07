package com.bankpaymentmonitor.paymentservice.payment.security;

import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlert;
import com.bankpaymentmonitor.paymentservice.payment.alert.PaymentAlertRepository;
import com.bankpaymentmonitor.paymentservice.payment.entity.Payment;
import com.bankpaymentmonitor.paymentservice.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchAccessServiceTest {

    @Mock
    private PaymentAlertRepository paymentAlertRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAlert paymentAlert;

    @Mock
    private Payment payment;

    private BranchAccessService branchAccessService;

    @BeforeEach
    void setUp() {
        branchAccessService = new BranchAccessService(
                paymentRepository
        );
    }

    @Test
    void shouldAllowUserToAccessOwnBranch() {

        CustomUserPrincipal principal = createBranchUser();

        boolean result =
                branchAccessService.canAccessBranch(
                        principal,
                        "BR-001"
                );

        assertTrue(result);
    }

    @Test
    void shouldRejectUserAccessingAnotherBranch() {

        CustomUserPrincipal principal = createBranchUser();

        boolean result =
                branchAccessService.canAccessBranch(
                        principal,
                        "BR-002"
                );

        assertFalse(result);
    }

    @Test
    void shouldAllowAdminToAccessAnyBranch() {

        CustomUserPrincipal principal = createAdminUser();;

        boolean result =
                branchAccessService.canAccessBranch(
                        principal,
                        "BR-002"
                );

        assertTrue(result);
    }
    @Test
    void shouldDenyUserAccessToPaymentAlertsFromAnotherBranch() {

        CustomUserPrincipal principal = createBranchUser();

        when(payment.getBranchCode())
                .thenReturn("BR-002");

        when(paymentRepository.findByReference("PAY-BR002-001"))
                .thenReturn(Optional.of(payment));

        boolean allowed =
                branchAccessService.canAccessPaymentAlerts(
                        principal,
                        "PAY-BR002-001"
                );

        assertFalse(allowed);
    }
    @Test
    void shouldAllowUserAccessToPaymentAlertsFromOwnBranch() {

        String paymentReference = "PAY-BR001-001";

        CustomUserPrincipal principal = createBranchUser();

        when(payment.getBranchCode())
                .thenReturn("BR-001");

        when(paymentRepository.findByReference(paymentReference))
                .thenReturn(Optional.of(payment));

        boolean allowed =
                branchAccessService.canAccessPaymentAlerts(
                        principal,
                        paymentReference
                );

        assertTrue(allowed);
    }
    @Test
    void shouldAllowAdminAccessToPaymentAlertsFromAnyBranch() {

        CustomUserPrincipal principal = createAdminUser();

        boolean allowed =
                branchAccessService.canAccessPaymentAlerts(
                        principal,
                        "PAY-BR002-001"
                );

        assertTrue(allowed);
    }
    @Test
    void shouldDenyUserAccessWhenPaymentAlertDoesNotExist() {

        CustomUserPrincipal principal = createBranchUser();

        when(paymentRepository.findByReference("UNKNOWN-PAYMENT"))
                .thenReturn(Optional.empty());

        boolean allowed =
                branchAccessService.canAccessPaymentAlerts(
                        principal,
                        "UNKNOWN-PAYMENT"
                );

        assertFalse(allowed);
    }
    @Test
    void shouldDenyUserAccessToPaymentFromAnotherBranch() {

        CustomUserPrincipal principal = createBranchUser();

        when(paymentRepository
                .findByReference("PAY-BR002-001"))
                .thenReturn(Optional.of(payment));

        when(payment.getBranchCode())
                .thenReturn("BR-002");

        boolean allowed =
                branchAccessService.canAccessPayment(
                        principal,
                        "PAY-BR002-001"
                );

        assertFalse(allowed);
    }
    @Test
    void shouldAllowUserAccessToPaymentFromOwnBranch() {

        CustomUserPrincipal principal = createBranchUser();

        when(payment.getBranchCode())
                .thenReturn("BR-001");

        when(paymentRepository.findByReference("PAY-001"))
                .thenReturn(Optional.of(payment));

        boolean allowed = branchAccessService.canAccessPayment(
                principal,
                "PAY-001"
        );

        assertTrue(allowed);
    }
    @Test
    void shouldAllowAdminAccessToPaymentFromAnyBranch() {

        CustomUserPrincipal principal =
                new CustomUserPrincipal(
                        new AppUser(
                                "admin",
                                "hashed-password",
                                null,
                                Role.ADMIN,
                                true
                        )
                );

        boolean allowed = branchAccessService.canAccessPayment(
                principal,
                "PAY-BR002-001"
        );

        assertTrue(allowed);

        verifyNoInteractions(paymentRepository);
    }
    @Test
    void shouldDenyUserAccessWhenPaymentDoesNotExist() {

        CustomUserPrincipal principal =
                new CustomUserPrincipal(
                        new AppUser(
                                "john",
                                "hashed-password",
                                "BR-001",
                                Role.USER,
                                true
                        )
                );

        when(paymentRepository.findByReference("UNKNOWN-PAYMENT"))
                .thenReturn(Optional.empty());

        boolean allowed = branchAccessService.canAccessPayment(
                principal,
                "UNKNOWN-PAYMENT"
        );

        assertFalse(allowed);
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
    void shouldAllowUserAccessToPaymentAlertsWhenPaymentBelongsToOwnBranchEvenIfNoAlertExists() {

        CustomUserPrincipal principal = createBranchUser();

        when(paymentRepository.findByReference("PAY-001"))
                .thenReturn(Optional.of(payment));

        when(payment.getBranchCode())
                .thenReturn("BR-001");

        boolean allowed =
                branchAccessService.canAccessPaymentAlerts(
                        principal,
                        "PAY-001"
                );

        assertTrue(allowed);
    }

}