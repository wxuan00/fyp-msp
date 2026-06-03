package com.msp.backend.modules.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
    }

    // ─── generateToken ────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken: returns non-blank token")
    void generateToken_returnsToken() {
        String token = jwtService.generateToken("user@test.com", "MERCHANT");
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("generateToken: different emails produce different tokens")
    void generateToken_differentEmailsDifferentTokens() {
        String t1 = jwtService.generateToken("a@test.com", "MERCHANT");
        String t2 = jwtService.generateToken("b@test.com", "MERCHANT");
        assertThat(t1).isNotEqualTo(t2);
    }

    // ─── extractUsername ──────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUsername: returns email from token")
    void extractUsername_returnsEmail() {
        String token = jwtService.generateToken("user@test.com", "MERCHANT");
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@test.com");
    }

    // ─── extractRole ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractRole: returns role from token")
    void extractRole_returnsRole() {
        String token = jwtService.generateToken("user@test.com", "ADMIN");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("extractRole: returns MERCHANT role correctly")
    void extractRole_merchant() {
        String token = jwtService.generateToken("merchant@test.com", "MERCHANT");
        assertThat(jwtService.extractRole(token)).isEqualTo("MERCHANT");
    }

    // ─── isTokenValid ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenValid: valid token returns true")
    void isTokenValid_validToken_true() {
        String token = jwtService.generateToken("user@test.com", "ADMIN");
        assertThat(jwtService.isTokenValid(token, "user@test.com")).isTrue();
    }

    @Test
    @DisplayName("isTokenValid: wrong username returns false")
    void isTokenValid_wrongUsername_false() {
        String token = jwtService.generateToken("user@test.com", "ADMIN");
        assertThat(jwtService.isTokenValid(token, "other@test.com")).isFalse();
    }

    @Test
    @DisplayName("isTokenValid: tampered token throws exception")
    void isTokenValid_tamperedToken_throws() {
        String token = jwtService.generateToken("user@test.com", "ADMIN");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThatThrownBy(() -> jwtService.isTokenValid(tampered, "user@test.com"))
                .isInstanceOf(Exception.class);
    }
}
