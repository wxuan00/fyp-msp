package com.msp.backend.modules.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit Tests for JwtService.
 * Tests JWT token generation, extraction, and validation.
 */
@DisplayName("JwtService — Unit Tests")
class JwtServiceUnitTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("returns a valid 3-part JWT token")
        void returnsValidJwt() {
            String token = jwtService.generateToken("user@test.com", "MERCHANT");
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("produces different tokens for different emails")
        void differentEmailsDifferentTokens() {
            String t1 = jwtService.generateToken("a@test.com", "MERCHANT");
            String t2 = jwtService.generateToken("b@test.com", "MERCHANT");
            assertThat(t1).isNotEqualTo(t2);
        }

        @Test
        @DisplayName("produces different tokens for different roles")
        void differentRolesDifferentTokens() {
            String t1 = jwtService.generateToken("user@test.com", "ADMIN");
            String t2 = jwtService.generateToken("user@test.com", "MERCHANT");
            assertThat(t1).isNotEqualTo(t2);
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsernameTests {

        @Test
        @DisplayName("extracts correct email from token")
        void extractsEmail() {
            String token = jwtService.generateToken("user@test.com", "MERCHANT");
            assertThat(jwtService.extractUsername(token)).isEqualTo("user@test.com");
        }
    }

    @Nested
    @DisplayName("extractRole")
    class ExtractRoleTests {

        @Test
        @DisplayName("extracts ADMIN role from token")
        void extractsAdminRole() {
            String token = jwtService.generateToken("admin@test.com", "ADMIN");
            assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("extracts MERCHANT role from token")
        void extractsMerchantRole() {
            String token = jwtService.generateToken("merchant@test.com", "MERCHANT");
            assertThat(jwtService.extractRole(token)).isEqualTo("MERCHANT");
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValidTests {

        @Test
        @DisplayName("returns true for valid token and matching username")
        void validTokenReturnsTrue() {
            String token = jwtService.generateToken("user@test.com", "ADMIN");
            assertThat(jwtService.isTokenValid(token, "user@test.com")).isTrue();
        }

        @Test
        @DisplayName("returns false for wrong username")
        void wrongUsernameReturnsFalse() {
            String token = jwtService.generateToken("user@test.com", "ADMIN");
            assertThat(jwtService.isTokenValid(token, "other@test.com")).isFalse();
        }

        @Test
        @DisplayName("throws exception for tampered token")
        void tamperedTokenThrows() {
            String token = jwtService.generateToken("user@test.com", "ADMIN");
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertThatThrownBy(() -> jwtService.isTokenValid(tampered, "user@test.com"))
                    .isInstanceOf(Exception.class);
        }
    }
}
