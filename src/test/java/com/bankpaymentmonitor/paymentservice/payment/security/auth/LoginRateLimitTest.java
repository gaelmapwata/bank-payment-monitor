package com.bankpaymentmonitor.paymentservice.payment.security.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoginRateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn429WhenLoginRateLimitIsExceeded() throws Exception {

        String requestBody = """
                {
                    "username": "unknown_user",
                    "password": "wrong_password"
                }
                """;

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                    post("/api/auth/login")
                            .servletPath("/api")
                            .with(request -> {
                                request.setRemoteAddr("192.0.2.10");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
            ).andExpect(status().isUnauthorized());
        }

        mockMvc.perform(
                post("/api/auth/login")
                        .servletPath("/api")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        ).andExpect(status().isTooManyRequests());
    }
    @Test
    void shouldNotBlockDifferentIpAddresses() throws Exception {

        String requestBody = """
            {
                "username": "unknown_user",
                "password": "wrong_password"
            }
            """;

        // Cinq tentatives depuis la première IP
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                    post("/api/auth/login")
                            .servletPath("/api")
                            .with(request -> {
                                request.setRemoteAddr("192.0.2.20");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
            ).andExpect(status().isUnauthorized());
        }

        // La première IP dépasse sa limite
        mockMvc.perform(
                post("/api/auth/login")
                        .servletPath("/api")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.20");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        ).andExpect(status().isTooManyRequests());

        // Une autre IP doit rester autorisée
        mockMvc.perform(
                post("/api/auth/login")
                        .servletPath("/api")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.21");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        ).andExpect(status().isUnauthorized());
    }
}