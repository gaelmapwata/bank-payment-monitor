    package com.bankpaymentmonitor.paymentservice.payment.security.jwt;

    import javax.crypto.SecretKey;

    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.oauth2.jwt.JwtDecoder;
    import org.springframework.security.oauth2.jwt.JwtEncoder;
    import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
    import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

    import javax.crypto.spec.SecretKeySpec;
    import java.nio.charset.StandardCharsets;

    @Configuration
    public class JwtConfig {

        @Bean
        public JwtEncoder jwtEncoder(
                @Value("${payment.security.jwt.secret}") String secret
        ) {

            SecretKey secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            return NimbusJwtEncoder
                    .withSecretKey(secretKey)
                    .build();
        }
        @Bean
        public JwtDecoder jwtDecoder(
                @Value("${payment.security.jwt.secret}") String secret
        ) {
            SecretKey secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            return NimbusJwtDecoder
                    .withSecretKey(secretKey)
                    .build();
        }
    }