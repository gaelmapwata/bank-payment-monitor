package com.bankpaymentmonitor.paymentservice.payment.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Clock;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final long expirationSeconds;

    public JwtService(
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${payment.security.jwt.expiration-seconds}")
            long expirationSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String username) {

        Instant now = Instant.now(clock);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }
}