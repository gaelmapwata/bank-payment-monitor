package com.bankpaymentmonitor.paymentservice.payment.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldForbidRegularUserFromCreatingAnotherUser()
            throws Exception {

        AppUser appUser = new AppUser(
                "john",
                "hashed-password",
                "BR-001",
                Role.USER,
                true
        );

        CustomUserPrincipal principal =
                new CustomUserPrincipal(appUser);

        String requestBody = """
                {
                    "username": "alice",
                    "password": "AliceTest123!",
                    "branchCode": "BR-001",
                    "role": "USER"
                }
                """;
        if (appUserRepository.findByUsername("alice").isEmpty()) {
            appUserRepository.save(
                    new AppUser(
                            "alice",
                            passwordEncoder.encode("AliceTest123!"),
                            "BR-001",
                            Role.USER,
                            true
                    )
            );
        }

        mockMvc.perform(
                        post("/api/users")
                                .servletPath("/api")
                                .with(user(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isForbidden());
    }
    @Test
    void shouldAllowAdminToCreateUser() throws Exception {

        AppUser admin = new AppUser(
                "admin",
                "hashed-password",
                null,
                Role.ADMIN,
                true
        );

        CustomUserPrincipal principal =
                new CustomUserPrincipal(admin);

        String requestBody = """
                {
                    "username": "test_create_user_001",
                    "password": "AliceTest123!",
                    "branchCode": "BR-001",
                    "role": "USER"
                }
            """;

        mockMvc.perform(
                        post("/api/users")
                                .servletPath("/api")
                                .with(user(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("test_create_user_001"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.branchCode").value("BR-001"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test
    void shouldReturnConflictWhenUsernameAlreadyExists() throws Exception {

        AppUser admin = new AppUser(
                "admin",
                "hashed-password",
                null,
                Role.ADMIN,
                true
        );

        CustomUserPrincipal principal =
                new CustomUserPrincipal(admin);

        String requestBody = """
            {
                "username": "alice",
                "password": "AliceTest123!",
                "branchCode": "BR-001",
                "role": "USER"
            }
            """;

        mockMvc.perform(
                        post("/api/users")
                                .servletPath("/api")
                                .with(user(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict());
    }
    @Test
    void shouldRejectUnauthenticatedUserCreation() throws Exception {

        String requestBody = """
            {
                "username": "unauthorized_user",
                "password": "Test1234!",
                "branchCode": "BR-001",
                "role": "USER"
            }
            """;

        mockMvc.perform(
                        post("/api/users")
                                .servletPath("/api")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldRejectUserCreationWithoutBranchCode() throws Exception {

        AppUser admin = new AppUser(
                "admin",
                "hashed-password",
                null,
                Role.ADMIN,
                true
        );

        CustomUserPrincipal principal =
                new CustomUserPrincipal(admin);

        String requestBody = """
            {
                "username": "user_without_branch",
                "password": "Test1234!",
                "role": "USER"
            }
            """;

        mockMvc.perform(
                        post("/api/users")
                                .servletPath("/api")
                                .with(user(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }
    @Test
    void shouldRejectAdminCreationWithBranchCode() throws Exception {

        AppUser admin = new AppUser(
                "admin",
                "hashed-password",
                null,
                Role.ADMIN,
                true
        );

        CustomUserPrincipal principal =
                new CustomUserPrincipal(admin);

        String requestBody = """
            {
                "username": "invalid_admin",
                "password": "AdminTest123!",
                "branchCode": "BR-001",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(
                        post("/api/users")
                                .servletPath("/api")
                                .with(user(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }
}