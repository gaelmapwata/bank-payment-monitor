package com.bankpaymentmonitor.paymentservice.payment.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppUserTest {

    @Test
    void shouldCreateUserWithBranch() {

        AppUser user = new AppUser(
                "john",
                "hashed-password",
                "BR-001",
                Role.USER,
                true
        );

        assertEquals("john", user.getUsername());
        assertEquals("BR-001", user.getBranchCode());
        assertEquals(Role.USER, user.getRole());
        assertTrue(user.isEnabled());
    }

    @Test
    void shouldRejectUserWithoutBranch() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new AppUser(
                        "john",
                        "hashed-password",
                        null,
                        Role.USER,
                        true
                )
        );
    }
    @Test
    void shouldCreateAdminWithoutBranch() {

        AppUser admin = new AppUser(
                "admin01",
                "hashed-password",
                null,
                Role.ADMIN,
                true
        );

        assertEquals("admin01", admin.getUsername());
        assertNull(admin.getBranchCode());
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(admin.isEnabled());
    }
    @Test
    void shouldRejectAdminWithBranch() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new AppUser(
                        "admin01",
                        "hashed-password",
                        "BR-001",
                        Role.ADMIN,
                        true
                )
        );
    }
    @Test
    void shouldChangeBranchForUser() {
        AppUser user = new AppUser(
                "john",
                "encoded-password",
                "BR-001",
                Role.USER,
                true
        );

        user.changeBranch("BR-002");

        assertEquals("BR-002", user.getBranchCode());
    }
    @Test
    void shouldRejectBlankBranchWhenChangingUserBranch() {
        AppUser user = new AppUser(
                "john",
                "encoded-password",
                "BR-001",
                Role.USER,
                true
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> user.changeBranch("   ")
        );
    }

    @Test
    void shouldRejectBranchChangeForAdmin() {
        AppUser admin = new AppUser(
                "admin",
                "encoded-password",
                null,
                Role.ADMIN,
                true
        );

        assertThrows(
                IllegalStateException.class,
                () -> admin.changeBranch("BR-002")
        );
    }
    @Test
    void shouldPromoteUserToAdmin() {
        AppUser user = new AppUser(
                "john",
                "encoded-password",
                "BR-001",
                Role.USER,
                true
        );

        user.promoteToAdmin();

        assertEquals(Role.ADMIN, user.getRole());
        assertNull(user.getBranchCode());
    }
    @Test
    void shouldDemoteAdminToUser() {
        AppUser admin = new AppUser(
                "admin",
                "encoded-password",
                null,
                Role.ADMIN,
                true
        );

        admin.demoteToUser("BR-002");

        assertEquals(Role.USER, admin.getRole());
        assertEquals("BR-002", admin.getBranchCode());
    }
    @Test
    void shouldRejectDemotionToUserWithoutBranch() {
        AppUser admin = new AppUser(
                "admin",
                "encoded-password",
                null,
                Role.ADMIN,
                true
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> admin.demoteToUser("   ")
        );
    }
}