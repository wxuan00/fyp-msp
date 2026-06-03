package com.msp.backend.modules.profile;

import com.msp.backend.modules.auth.TotpService;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    private final com.msp.backend.modules.merchant.MerchantUserMappingRepository merchantUserMappingRepository;
    private final com.msp.backend.modules.merchant.MerchantRepository merchantRepository;

    // Get own profile
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile() {
        User user = getCurrentUser();
        userService.populateRole(user);
        // Don't return password
        user.setPassword(null);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("userId", user.getUserId());
        result.put("firstName", user.getFirstName());
        result.put("lastName", user.getLastName());
        result.put("displayName", user.getDisplayName());
        result.put("email", user.getEmail());
        result.put("contactNumber", user.getContactNumber());
        result.put("role", user.getRole());
        result.put("status", user.getStatus());
        result.put("mfaEnabled", user.isMfaEnabled());
        result.put("mustChangePassword", user.getMustChangePassword());
        result.put("createdAt", user.getCreatedAt());
        result.put("lastLoginAt", user.getLastLoginAt());

        // Include linked merchants
        var mappings = merchantUserMappingRepository.findByUserId(user.getUserId());
        var linkedMerchants = mappings.stream().map(m -> {
            Map<String, Object> merchant = new java.util.LinkedHashMap<>();
            merchant.put("merchantId", m.getMerchantId());
            merchantRepository.findById(m.getMerchantId()).ifPresent(me -> {
                merchant.put("merchantName", me.getMerchantName());
                merchant.put("status", me.getStatus());
            });
            return merchant;
        }).collect(java.util.stream.Collectors.toList());
        result.put("linkedMerchants", linkedMerchants);

        return ResponseEntity.ok(result);
    }

    // Update own profile (name, phone, display name)
    @PutMapping
    public ResponseEntity<User> updateProfile(@RequestBody Map<String, String> updates) {
        User user = getCurrentUser();

        if (updates.containsKey("firstName")) user.setFirstName(updates.get("firstName"));
        if (updates.containsKey("lastName")) user.setLastName(updates.get("lastName"));
        if (updates.containsKey("displayName")) user.setDisplayName(updates.get("displayName"));
        if (updates.containsKey("contactNumber")) user.setContactNumber(updates.get("contactNumber"));

        User saved = userRepository.save(user);
        saved.setPassword(null);
        return ResponseEntity.ok(saved);
    }

    // Change own password
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> passwords) {
        User user = getCurrentUser();

        String currentPassword = passwords.get("currentPassword");
        String newPassword = passwords.get("newPassword");

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(Boolean.FALSE); // clear first-login flag
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // ============ MFA Setup Endpoints ============

    /**
     * Generate a new MFA secret and QR code for setup
     * Does NOT enable MFA yet - user must verify a code first
     */
    @PostMapping("/mfa/setup")
    public ResponseEntity<Map<String, Object>> setupMfa() {
        User user = getCurrentUser();

        // Generate a new secret key
        String secret = totpService.generateSecret();
        
        // Generate QR code data URI
        String qrCodeDataUri = totpService.generateQrCodeDataUri(secret, user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("secret", secret);
        response.put("qrCode", qrCodeDataUri);
        response.put("message", "Scan the QR code with Google Authenticator, then verify with a code");

        return ResponseEntity.ok(response);
    }

    /**
     * Verify OTP code and enable MFA
     */
    @PostMapping("/mfa/enable")
    public ResponseEntity<Map<String, Object>> enableMfa(@RequestBody Map<String, String> request) {
        User user = getCurrentUser();

        String secret = request.get("secret");
        String code = request.get("code");

        if (secret == null || secret.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Secret key is required"
            ));
        }

        if (code == null || code.length() != 6) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid verification code. Must be 6 digits."
            ));
        }

        // Verify the code with the provided secret
        if (!totpService.verifyCode(secret, code)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid verification code. Please try again."
            ));
        }

        // Code is valid - save the secret and enable MFA
        user.setSecretKey(secret);
        user.setMfaEnabled(true);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "MFA has been enabled successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Disable MFA for the current user
     * Requires password verification for security
     */
    @PostMapping("/mfa/disable")
    public ResponseEntity<Map<String, Object>> disableMfa(@RequestBody Map<String, String> request) {
        User user = getCurrentUser();

        String password = request.get("password");
        String code = request.get("code");

        // Require password verification
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid password"
            ));
        }

        // Optionally require current MFA code
        if (user.isMfaEnabled() && user.getSecretKey() != null) {
            if (code == null || !totpService.verifyCode(user.getSecretKey(), code)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid MFA verification code"
                ));
            }
        }

        // Disable MFA
        user.setMfaEnabled(false);
        user.setSecretKey(null);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "MFA has been disabled");

        return ResponseEntity.ok(response);
    }

    /**
     * Get current MFA status
     */
    @GetMapping("/mfa/status")
    public ResponseEntity<Map<String, Object>> getMfaStatus() {
        User user = getCurrentUser();

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", user.isMfaEnabled());
        response.put("configured", user.getSecretKey() != null && !user.getSecretKey().isBlank());

        return ResponseEntity.ok(response);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
