package com.bankpaymentmonitor.paymentservice.payment.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Value("${payment.security.jwt.secret}")
    private String jwtSecret;

    @Test
    void shouldGenerateTokenForUsername() {

        String token = jwtService.generateToken("john");

        assertNotNull(token);
        assertFalse(token.isBlank());

        Jwt decodedJwt = jwtDecoder.decode(token);

        assertEquals("john", decodedJwt.getSubject());
    }
    @Test
    void shouldGenerateTokenThatExpiresOneHourAfterIssuedAt() {

        String token = jwtService.generateToken("john");

        Jwt decodedJwt = jwtDecoder.decode(token);

        Instant issuedAt = decodedJwt.getIssuedAt();
        Instant expiresAt = decodedJwt.getExpiresAt();

        assertNotNull(issuedAt);
        assertNotNull(expiresAt);

        assertEquals(
                issuedAt.plusSeconds(3600),
                expiresAt
        );
    }
    @Test
    void shouldRejectExpiredToken() {

        // 1. JwtService génère un vrai token signé
        String token = jwtService.generateToken("john");

        // 2. On recrée la même clé uniquement pour CE test
        SecretKey secretKey = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        // 3. Decoder totalement indépendant du bean Spring
        NimbusJwtDecoder testDecoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .build();

        // 4. Pour ce decoder uniquement, on avance le temps de 2 heures
        Instant future = Instant.now()
                .plusSeconds(7200);

        JwtTimestampValidator timestampValidator =
                new JwtTimestampValidator();

        timestampValidator.setClock(
                Clock.fixed(future, ZoneOffset.UTC)
        );

        testDecoder.setJwtValidator(timestampValidator);

        // 5. Le token doit maintenant être considéré comme expiré
        assertThrows(
                JwtException.class,
                () -> testDecoder.decode(token)
        );
    }

}