package com.bankpaymentmonitor.paymentservice.payment.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@Profile("dev")
public class DevUserInitializer {

    @Bean
    CommandLineRunner initializeDevUser(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.dev.admin.password:}") String adminPassword
    ) {
        return args -> {

            // Initialisation de l'utilisateur de développement existant
            if (appUserRepository.findByUsername("john").isEmpty()) {

                AppUser john = new AppUser(
                        "john",
                        passwordEncoder.encode("Test1234!"),
                        "BR-001",
                        Role.USER,
                        true
                );

                appUserRepository.save(john);
            }

            // Initialisation du premier administrateur
            if (appUserRepository.findByUsername("admin").isEmpty()) {

                if (adminPassword == null || adminPassword.isBlank()) {
                    throw new IllegalStateException(
                            "APP_DEV_ADMIN_PASSWORD is required to initialize the development admin"
                    );
                }

                AppUser admin = new AppUser(
                        "admin",
                        passwordEncoder.encode(adminPassword),
                        null,
                        Role.ADMIN,
                        true
                );

                appUserRepository.save(admin);
            }
        };
    }
}