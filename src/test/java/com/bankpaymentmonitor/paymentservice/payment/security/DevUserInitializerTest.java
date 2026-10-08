package com.bankpaymentmonitor.paymentservice.payment.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DevUserInitializerTest {

    @Test
    void shouldCreateAdminWhenAdminDoesNotExist() throws Exception {

        AppUserRepository repository = mock(AppUserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        when(repository.findByUsername("john"))
                .thenReturn(Optional.of(
                        new AppUser(
                                "john",
                                passwordEncoder.encode("Test1234!"),
                                "BR-001",
                                Role.USER,
                                true
                        )
                ));

        when(repository.findByUsername("admin"))
                .thenReturn(Optional.empty());

        DevUserInitializer initializer = new DevUserInitializer();

        CommandLineRunner runner = initializer.initializeDevUser(
                repository,
                passwordEncoder,
                "AdminTest123!"
        );

        runner.run();

        verify(repository).save(argThat(user ->
                user.getUsername().equals("admin")
                        && user.getRole() == Role.ADMIN
                        && user.getBranchCode() == null
                        && user.isEnabled()
                        && passwordEncoder.matches("AdminTest123!", user.getPassword())
        ));
    }
}