package com.bankpaymentmonitor.paymentservice.payment.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevUserInitializer {

    @Bean
    CommandLineRunner initializeDevUser(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

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
        };
    }
}