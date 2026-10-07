package com.bankpaymentmonitor.paymentservice.payment.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String branchCode;
    private final Role role;
    private final boolean enabled;

    public CustomUserPrincipal(AppUser appUser) {
        this.id = appUser.getId();
        this.username = appUser.getUsername();
        this.password = appUser.getPassword();
        this.branchCode = appUser.getBranchCode();
        this.role = appUser.getRole();
        this.enabled = appUser.isEnabled();
    }

    public Long getId() {
        return id;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" + role.name()
                )
        );
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}