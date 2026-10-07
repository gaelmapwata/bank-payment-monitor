package com.bankpaymentmonitor.paymentservice.payment.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Test
    void shouldEncodeAndMatchPassword() {

        String rawPassword = "Test1234!";

        String encodedPassword =
                passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);

        assertTrue(
                passwordEncoder.matches(
                        rawPassword,
                        encodedPassword
                )
        );

        assertFalse(
                passwordEncoder.matches(
                        "WrongPassword",
                        encodedPassword
                )
        );
    }
}