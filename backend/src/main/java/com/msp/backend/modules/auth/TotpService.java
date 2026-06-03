package com.msp.backend.modules.auth;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class TotpService {

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;

    public TotpService() {
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();
        
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    /**
     * Generate a new secret key for a user
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Generate a QR code data URI for Google Authenticator setup
     * @param secret The user's secret key
     * @param email The user's email (used as the account name)
     * @return Data URI string for the QR code image
     */
    public String generateQrCodeDataUri(String secret, String email) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("Merchant Service Portal")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        try {
            byte[] imageData = qrGenerator.generate(data);
            return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            throw new RuntimeException("Error generating QR code", e);
        }
    }

    /**
     * Verify a TOTP code against a secret
     * @param secret The user's secret key
     * @param code The 6-digit code from Google Authenticator
     * @return true if the code is valid
     */
    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
}
