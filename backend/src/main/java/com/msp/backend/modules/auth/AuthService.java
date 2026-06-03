package com.msp.backend.modules.auth;

import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.modules.auth.dto.LoginRequest;
import com.msp.backend.modules.auth.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new RuntimeException("Email or display name is required");
        }

        // Try email first, then display name
        User user = userRepository.findByEmailAndDeletedAtIsNull(identifier)
                .or(() -> userRepository.findByDisplayNameIgnoreCaseAndDeletedAtIsNull(identifier))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (user.getDeletedAt() != null) {
            throw new RuntimeException("Invalid credentials");
        }

        if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Account inactive. Please contact admin for assistance.");
        }

        if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Account suspended. Please contact admin for assistance.");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Account is not active. Please contact admin for assistance.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        userService.populateRole(user);

        if (user.getRole() == null || user.getRole().isBlank()) {
            throw new RuntimeException("User has no role assigned");
        }

        // Record last login time
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(jwtToken)
                .role(user.getRole())
                .build();
    }

    @Transactional
    public Map<String, Object> forgotPassword(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);

        // Always return success to prevent email enumeration
        Map<String, Object> result = new HashMap<>();
        if (user == null || user.getDeletedAt() != null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            result.put("message", "If that email exists in our system, a reset link has been sent.");
            return result;
        }

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // In production: send email with reset link. For dev: return token directly.
        result.put("message", "If that email exists in our system, a reset link has been sent.");
        result.put("resetToken", token); // Dev only – remove when email service is configured
        return result;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Invalid or expired reset token.");
        }
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token."));

        if (user.getResetTokenExpiry() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new RuntimeException("Reset token has expired. Please request a new one.");
        }

        userService.resetPassword(user.getUserId(), newPassword);

        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }
}