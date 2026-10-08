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

        // GIVEN
        String token = jwtService.generateToken("john");

        String[] parts = token.split("\\.");

        assertEquals(3, parts.length);

        // Modifier un caractère du payload encodé
        String payload = parts[1];

        String tamperedPayload =
                (payload.charAt(0) == 'a' ? "b" : "a")
                        + payload.substring(1);

        // Reconstruire le JWT avec sa signature originale
        String tamperedToken =
                parts[0] + "." + tamperedPayload + "." + parts[2];

        // WHEN / THEN
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
    @Test
    void shouldUseCurrentUserBranchAfterBranchChange() throws Exception {

        String username = "branch-change-user";

        AppUser user = new AppUser(
                username,
                passwordEncoder.encode("Test1234!"),
                "BR-001",
                Role.USER,
                true
        );

        appUserRepository.save(user);

        try {
            // Le JWT est créé pendant que l'utilisateur appartient à BR-001
            String token = jwtService.generateToken(username);

            // Après création du JWT, l'utilisateur est déplacé vers BR-002
            user.changeBranch("BR-002");
            appUserRepository.save(user);

            // Le même JWT ne doit plus donner accès à BR-001
            mockMvc.perform(
                            get("/api/payments/branch/BR-001")
                                    .servletPath("/api")
                                    .header(
                                            "Authorization",
                                            "Bearer " + token
                                    )
                    )
                    .andExpect(status().isForbidden());
            // 4. Le même token doit maintenant autoriser BR-002
            mockMvc.perform(
                            get("/api/payments/branch/BR-002")
                                    .servletPath("/api")
                                    .header(
                                            "Authorization",
                                            "Bearer " + token
                                    )
                    )
                    .andExpect(status().isOk());

        } finally {
            appUserRepository.delete(user);
        }
    }
    @Test
    void shouldUseCurrentUserRoleAfterPromotionToAdmin() throws Exception {

        String username = "promoted-admin-user";

        AppUser user = new AppUser(
                username,
                passwordEncoder.encode("Test1234!"),
                "BR-001",
                Role.USER,
                true
        );

        appUserRepository.save(user);

        try {
            // 1. JWT créé lorsque l'utilisateur est encore USER / BR-001
            String token = jwtService.generateToken(username);

            // 2. Promotion APRÈS émission du JWT
            user.promoteToAdmin();
            appUserRepository.save(user);

            // 3. Le même JWT doit maintenant avoir les droits ADMIN
            // Un ADMIN peut accéder à n'importe quelle agence.
            mockMvc.perform(
                            get("/api/payments/branch/BR-999")
                                    .servletPath("/api")
                                    .header(
                                            "Authorization",
                                            "Bearer " + token
                                    )
                    )
                    .andExpect(status().isOk());

        } finally {
            appUserRepository.delete(user);
        }
    }
    @Test
    void shouldUseCurrentUserRoleAfterDemotionToUser() throws Exception {

        String username = "demoted-admin-user";

        AppUser admin = new AppUser(
                username,
                passwordEncoder.encode("Test1234!"),
                null,
                Role.ADMIN,
                true
        );

        appUserRepository.save(admin);

        try {
            // 1. JWT créé lorsque l'utilisateur est encore ADMIN
            String token = jwtService.generateToken(username);

            // 2. Rétrogradation APRÈS émission du JWT
            admin.demoteToUser("BR-002");
            appUserRepository.save(admin);

            // 3. Le même JWT ne doit plus donner accès
            //    à une autre agence.
            mockMvc.perform(
                            get("/api/payments/branch/BR-999")
                                    .servletPath("/api")
                                    .header(
                                            "Authorization",
                                            "Bearer " + token
                                    )
                    )
                    .andExpect(status().isForbidden());

            // 4. Mais le USER doit avoir accès à sa nouvelle agence.
            mockMvc.perform(
                            get("/api/payments/branch/BR-002")
                                    .servletPath("/api")
                                    .header(
                                            "Authorization",
                                            "Bearer " + token
                                    )
                    )
                    .andExpect(status().isOk());

        } finally {
            appUserRepository.delete(admin);
        }
    }
}