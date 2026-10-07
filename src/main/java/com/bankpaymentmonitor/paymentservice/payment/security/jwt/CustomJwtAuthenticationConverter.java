package com.bankpaymentmonitor.paymentservice.payment.security.jwt;

import com.bankpaymentmonitor.paymentservice.payment.security.CustomUserDetailsService;
import com.bankpaymentmonitor.paymentservice.payment.security.CustomUserPrincipal;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final CustomUserDetailsService userDetailsService;

    public CustomJwtAuthenticationConverter(
            CustomUserDetailsService userDetailsService
    ) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        String username = jwt.getSubject();

        CustomUserPrincipal principal =
                (CustomUserPrincipal) userDetailsService
                        .loadUserByUsername(username);
        if (!principal.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }
}