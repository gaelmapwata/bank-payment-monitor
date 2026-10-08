package com.bankpaymentmonitor.paymentservice.payment.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
@Component
public class CurrentUserProvider {

    public String getUsername() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserPrincipal principal)) {

            throw new IllegalStateException(
                    "No authenticated application user found"
            );
        }

        return principal.getUsername();
    }
    public String getBranchCode() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserPrincipal principal)) {

            throw new IllegalStateException(
                    "No authenticated application user found"
            );
        }

        return principal.getBranchCode();
    }

}