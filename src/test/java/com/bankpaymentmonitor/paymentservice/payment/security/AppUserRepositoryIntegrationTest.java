package com.bankpaymentmonitor.paymentservice.payment.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Transactional
class AppUserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17")
                    .withDatabaseName("payment_test_db")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void shouldSaveAndFindUserByUsername() {

        AppUser user = new AppUser(
                "john",
                "hashed-password",
                "BR-001",
                Role.USER,
                true
        );

        appUserRepository.saveAndFlush(user);

        Optional<AppUser> result =
                appUserRepository.findByUsername("john");

        assertTrue(result.isPresent());

        AppUser savedUser = result.get();

        assertNotNull(savedUser.getId());
        assertEquals("john", savedUser.getUsername());
        assertEquals("BR-001", savedUser.getBranchCode());
        assertEquals(Role.USER, savedUser.getRole());
        assertTrue(savedUser.isEnabled());
    }
    @Test
    void shouldSaveAdminWithoutBranch() {

        AppUser admin = new AppUser(
                "admin01",
                "hashed-password",
                null,
                Role.ADMIN,
                true
        );

        appUserRepository.saveAndFlush(admin);

        Optional<AppUser> result =
                appUserRepository.findByUsername("admin01");

        assertTrue(result.isPresent());

        AppUser savedAdmin = result.get();

        assertNotNull(savedAdmin.getId());
        assertEquals("admin01", savedAdmin.getUsername());
        assertEquals(Role.ADMIN, savedAdmin.getRole());
        assertNull(savedAdmin.getBranchCode());
        assertTrue(savedAdmin.isEnabled());
    }

}