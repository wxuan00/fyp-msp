package com.msp.backend.modules.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TotpService Unit Tests")
class TotpServiceTest {

    private TotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpService();
    }

    // ─── generateSecret ───────────────────────────────────────────────────────

    @Test
    @DisplayName("generateSecret: returns non-blank secret")
    void generateSecret_returnsNonBlank() {
        String secret = totpService.generateSecret();
        assertThat(secret).isNotBlank();
    }

    @Test
    @DisplayName("generateSecret: generates unique secrets")
    void generateSecret_uniqueSecrets() {
        String s1 = totpService.generateSecret();
        String s2 = totpService.generateSecret();
        assertThat(s1).isNotEqualTo(s2);
    }

    @Test
    @DisplayName("generateSecret: secret is reasonably long (>=16 chars)")
    void generateSecret_sufficientLength() {
        String secret = totpService.generateSecret();
        assertThat(secret.length()).isGreaterThanOrEqualTo(16);
    }

    // ─── generateQrCodeDataUri ────────────────────────────────────────────────

    @Test
    @DisplayName("generateQrCodeDataUri: returns data URI with image prefix")
    void generateQrCode_returnsDataUri() {
        String secret = totpService.generateSecret();
        String uri = totpService.generateQrCodeDataUri(secret, "user@test.com");
        assertThat(uri).startsWith("data:image/");
    }

    @Test
    @DisplayName("generateQrCodeDataUri: different emails produce different QR URIs")
    void generateQrCode_differentEmailsDifferentUri() {
        String secret = totpService.generateSecret();
        String uri1 = totpService.generateQrCodeDataUri(secret, "a@test.com");
        String uri2 = totpService.generateQrCodeDataUri(secret, "b@test.com");
        assertThat(uri1).isNotEqualTo(uri2);
    }

    // ─── verifyCode ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyCode: invalid code returns false")
    void verifyCode_invalidCode_returnsFalse() {
        String secret = totpService.generateSecret();
        // Any random 6-digit code that is almost certainly wrong
        assertThat(totpService.verifyCode(secret, "000000")).isFalse();
    }

    @Test
    @DisplayName("verifyCode: non-numeric code returns false")
    void verifyCode_nonNumericCode_returnsFalse() {
        String secret = totpService.generateSecret();
        assertThat(totpService.verifyCode(secret, "ABCDEF")).isFalse();
    }
}
