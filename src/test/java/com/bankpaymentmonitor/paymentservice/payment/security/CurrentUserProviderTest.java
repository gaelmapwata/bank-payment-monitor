package com.bankpaymentmonitor.paymentservice.payment.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserProviderTest {
    @Test
    void shouldReturnAuthenticatedUserBranchCode() {

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

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        try {
            CurrentUserProvider provider =
                    new CurrentUserProvider();

            assertEquals(
                    "BR-001",
                    provider.getBranchCode()
            );

        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    @Test
    void shouldReturnNullBranchCodeForAdmin() {

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

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        try {
            CurrentUserProvider provider =
                    new CurrentUserProvider();

            assertNull(provider.getBranchCode());

        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    @Test
    void shouldRejectBranchAccessWhenUserIsNotAuthenticated() {

        SecurityContextHolder.clearContext();

        CurrentUserProvider provider =
                new CurrentUserProvider();

        assertThrows(
                IllegalStateException.class,
                provider::getBranchCode
        );
    }
    @Test
    void shouldRejectAnonymousUserWhenGettingUsername() {

        var authentication = new AnonymousAuthenticationToken(
                "test-key",
                "anonymousUser",
                List.of(
                        new SimpleGrantedAuthority("ROLE_ANONYMOUS")
                )
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        try {
            CurrentUserProvider provider =
                    new CurrentUserProvider();

            assertThrows(
                    IllegalStateException.class,
                    provider::getUsername
            );

        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    @Test
    void shouldRejectUsernameWhenPrincipalIsNotCustomUserPrincipal() {

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "john",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_USER")
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        try {
            CurrentUserProvider provider =
                    new CurrentUserProvider();

            assertThrows(
                    IllegalStateException.class,
                    provider::getUsername
            );

        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    @Test
    void shouldReturnAuthenticatedUsername() {

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

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        try {
            CurrentUserProvider provider =
                    new CurrentUserProvider();

            assertEquals("john", provider.getUsername());

        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    @Test
    void shouldReturnAuthenticatedUserRole() {

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

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        try {
            CurrentUserProvider provider =
                    new CurrentUserProvider();
            // WHEN
            Role role = provider.getRole();

            // THEN
            assertEquals(Role.USER, role);

        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}