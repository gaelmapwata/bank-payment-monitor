package com.bankpaymentmonitor.paymentservice.payment.security.auth;

import com.bankpaymentmonitor.paymentservice.payment.security.AppUser;
import com.bankpaymentmonitor.paymentservice.payment.security.AppUserRepository;
import com.bankpaymentmonitor.paymentservice.payment.security.Role;
import com.bankpaymentmonitor.paymentservice.payment.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldLoginWithValidCredentials() throws Exception {

        String requestBody = """
            {
                "username": "john",
                "password": "Test1234!"
            }
            """;

        String responseBody = mockMvc.perform(
                        post("/api/auth/login")
                                .servletPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginResponse loginResponse =
                objectMapper.readValue(responseBody, LoginResponse.class);

        Jwt jwt = jwtDecoder.decode(loginResponse.token());

        assertEquals("john", jwt.getSubject());
    }
    @Test
        void shouldRejectLoginWithInvalidCredentials() throws Exception {

        String requestBody = """
            {
                "username": "john",
                "password": "wrong-password"
            }
            """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .servletPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldAccessProtectedEndpointWithValidBearerToken() throws Exception {

        String token = jwtService.generateToken("john");

        mockMvc.perform(
                        get("/api/payments/status/PENDING")
                                .servletPath("/api")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk());
    }
    @Test
    void shouldRejectBasicAuthenticationOnProtectedEndpoint() throws Exception {

        mockMvc.perform(
                        get("/api/payments/status/PENDING")
                                .servletPath("/api")
                                .with(httpBasic("john", "Test1234!"))
                )
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldRejectTamperedBearerToken() throws Exception {

        String token = jwtService.generateToken("john");

        String tamperedToken =
                token.substring(0, token.length() - 1)
                        + (token.endsWith("a") ? "b" : "a");

        mockMvc.perform(
                        get("/api/payments/status/PENDING")
                                .servletPath("/api")
                                .header(
                                        "Authorization",
                                        "Bearer " + tamperedToken
                                )
                )
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldRejectExpiredBearerToken() throws Exception {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("john")
                .issuedAt(now.minusSeconds(7200))
                .expiresAt(now.minusSeconds(3600))
                .build();

        String expiredToken = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        mockMvc.perform(
                        get("/api/payments/status/PENDING")
                                .servletPath("/api")
                                .header(
                                        "Authorization",
                                        "Bearer " + expiredToken
                                )
                )
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldRejectValidBearerTokenWhenUserIsDisabled() throws Exception {

        String username = "disabled-user";

        AppUser disabledUser = new AppUser(
                username,
                passwordEncoder.encode("Test1234!"),
                "BR-001",
                Role.USER,
                false
        );

        appUserRepository.save(disabledUser);

        try {
            String token = jwtService.generateToken(username);

            mockMvc.perform(
                            get("/api/payments/status/PENDING")
                                    .servletPath("/api")
                                    .header(
                                            "Authorization",
                                            "Bearer " + token
                                    )
                    )
                    .andExpect(status().isUnauthorized());

        } finally {
            appUserRepository.delete(disabledUser);
        }
    }
    @Test
    void shouldRejectValidBearerTokenWhenUserDoesNotExist() throws Exception {

        String username = "ghost-user";

        String token = jwtService.generateToken(username);

        mockMvc.perform(
                        get("/api/payments/status/PENDING")
                                .servletPath("/api")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isUnauthorized());
    }
}