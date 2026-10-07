package com.bankpaymentmonitor.paymentservice.payment.security;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "app_users")
@Getter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "branch_code")
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    protected AppUser() {
        // Required by JPA
    }

    public AppUser(
            String username,
            String password,
            String branchCode,
            Role role,
            boolean enabled
    ) {
        if (role == Role.USER &&
                (branchCode == null || branchCode.isBlank())) {
            throw new IllegalArgumentException(
                    "A USER must belong to a branch"
            );
        }
        if (role == Role.ADMIN &&
                branchCode != null &&
                !branchCode.isBlank()) {
            throw new IllegalArgumentException(
                    "An ADMIN must not belong to a branch"
            );
        }
        this.username = username;
        this.password = password;
        this.branchCode = branchCode;
        this.role = role;
        this.enabled = enabled;
    }
    public void changeBranch(String newBranchCode) {
        if (role != Role.USER) {
            throw new IllegalStateException(
                    "Only a USER can be assigned to a branch"
            );
        }

        if (newBranchCode == null || newBranchCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Branch code is required for a USER"
            );
        }

        this.branchCode = newBranchCode;
    }
    public void promoteToAdmin() {
        this.role = Role.ADMIN;
        this.branchCode = null;
    }
    public void demoteToUser(String branchCode) {
        if (branchCode == null || branchCode.isBlank()) {
            throw new IllegalArgumentException(
                    "A USER must belong to a branch"
            );
        }

        this.role = Role.USER;
        this.branchCode = branchCode;
    }

}