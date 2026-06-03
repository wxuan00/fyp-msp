package com.msp.backend.modules.auth;


import com.msp.backend.modules.auth.dto.LoginRequest;
import com.msp.backend.modules.auth.dto.AuthResponse;
import com.msp.backend.modules.role.Permission;
import com.msp.backend.modules.role.PermissionRepository;
import com.msp.backend.modules.role.Role;
import com.msp.backend.modules.role.RolePermission;
import com.msp.backend.modules.role.RolePermissionRepository;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserRole;
import com.msp.backend.modules.user.UserRoleRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.util.MerchantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantResolver merchantResolver;
    private final TotpService totpService;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        AuthResponse authResponse;
        try {
            authResponse = authService.login(request);
        } catch (RuntimeException ex) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        }

        // Resolve user by identifier (email or display name)
        String identifier = request.getIdentifier();
        User user = userRepository.findByEmailAndDeletedAtIsNull(identifier)
                .or(() -> userRepository.findByDisplayNameIgnoreCaseAndDeletedAtIsNull(identifier))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("token", authResponse.getToken());
        response.put("role", authResponse.getRole());

        if (user.isMfaEnabled() && user.getSecretKey() != null && !user.getSecretKey().isBlank()) {
            response.put("mfaRequired", true);
        } else {
            response.put("mfaRequired", false);
        }

        return ResponseEntity.ok(response);
    }

    // Get current logged-in user info
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> userInfo = new HashMap<>();
        userService.populateRole(user);
        userInfo.put("id", user.getUserId());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("displayName", user.getDisplayName());
        userInfo.put("role", user.getRole());
        userInfo.put("contactNumber", user.getContactNumber());
        userInfo.put("status", user.getStatus());
        // Look up merchantId via both merchants.user_id and merchant_users mapping table
        Long merchantId = merchantResolver.resolveForUser(user);
        userInfo.put("merchantId", merchantId);
        userInfo.put("mfaEnabled", user.isMfaEnabled());
        userInfo.put("mustChangePassword", Boolean.TRUE.equals(user.getMustChangePassword()));

        // Collect permissions:
        // If user has custom BUSINESS roles (non-base), use ONLY those — they restrict base MERCHANT permissions.
        // Otherwise use all assigned roles (covers ADMIN/SYSTEM users and bare MERCHANT users).
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getUserId());

        List<UserRole> customBusinessRoles = userRoles.stream()
                .filter(ur -> roleRepository.findById(ur.getRoleId())
                        .map(r -> "BUSINESS".equalsIgnoreCase(r.getRoleType())
                                  && !"MERCHANT".equalsIgnoreCase(r.getRoleName()))
                        .orElse(false))
                .collect(Collectors.toList());

        List<UserRole> rolesToUse = customBusinessRoles.isEmpty() ? userRoles : customBusinessRoles;

        List<String> permissionNames = new ArrayList<>();
        for (UserRole ur : rolesToUse) {
            List<Long> permIds = rolePermissionRepository.findByRoleId(ur.getRoleId())
                    .stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
            if (!permIds.isEmpty()) {
                permissionRepository.findAllById(permIds).forEach(p -> {
                    if (!permissionNames.contains(p.getPermissionName())) {
                        permissionNames.add(p.getPermissionName());
                    }
                });
            }
        }
        userInfo.put("permissions", permissionNames);
        return userInfo;
    }

    @PatchMapping("/clear-must-change-password")
    public ResponseEntity<?> clearMustChangePassword() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setMustChangePassword(false);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password change requirement cleared"));
    }

    // MFA verification endpoint
    @PostMapping("/mfa/verify")
    public ResponseEntity<Map<String, Object>> verifyMfa(@RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otpCode = request.get("code");
        if (otpCode == null || otpCode.length() != 6) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid OTP code. Must be 6 digits."
            ));
        }

        // Verify the OTP against the user's secret key
        if (user.getSecretKey() == null || user.getSecretKey().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "MFA is not configured for this account. Please set up MFA in your profile."
            ));
        }

        // Verify TOTP code using Google Authenticator compatible algorithm
        boolean isValid = totpService.verifyCode(user.getSecretKey(), otpCode);
        
        Map<String, Object> response = new HashMap<>();
        if (isValid) {
            response.put("success", true);
            response.put("message", "MFA verification successful");
        } else {
            response.put("success", false);
            response.put("message", "Invalid verification code. Please try again.");
        }
        return ResponseEntity.ok(response);
    }

    // Forgot password - generate reset token
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }
        Map<String, Object> result = authService.forgotPassword(email.trim().toLowerCase());
        return ResponseEntity.ok(result);
    }

    // Reset password using token
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token and new password are required."));
        }
        try {
            authService.resetPassword(token.trim(), newPassword);
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}