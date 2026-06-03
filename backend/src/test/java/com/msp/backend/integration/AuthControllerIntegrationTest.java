package com.msp.backend.integration;

import com.msp.backend.modules.auth.AuthService;
import com.msp.backend.modules.auth.AuthController;
import com.msp.backend.modules.auth.TotpService;
import com.msp.backend.modules.auth.dto.AuthResponse;
import com.msp.backend.modules.auth.dto.LoginRequest;
import com.msp.backend.modules.role.RolePermissionRepository;
import com.msp.backend.modules.role.PermissionRepository;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.modules.user.UserRoleRepository;
import com.msp.backend.util.MerchantResolver;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — Integration Tests")
class AuthControllerIntegrationTest {

    @Mock private AuthService authService;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private MerchantResolver merchantResolver;
    @Mock private TotpService totpService;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private RoleRepository roleRepository;

    @InjectMocks private AuthController authController;

    @Nested @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test @DisplayName("returns error for invalid credentials")
        void invalidCredentials() {
            LoginRequest req = new LoginRequest();
            req.setEmail("bad@test.com");
            req.setPassword("wrong");
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new RuntimeException("Invalid credentials"));
            ResponseEntity<Map<String, Object>> resp = authController.login(req);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resp.getBody()).containsKey("message");
        }

        @Test @DisplayName("returns token on successful login without MFA")
        void successfulLoginNoMfa() {
            LoginRequest req = new LoginRequest();
            req.setEmail("admin@msp.com");
            req.setPassword("pass");
            AuthResponse authRes = AuthResponse.builder().token("jwt").role("ADMIN").build();
            when(authService.login(any(LoginRequest.class))).thenReturn(authRes);
            User u = new User(); u.setEmail("admin@msp.com"); u.setMfaEnabled(false);
            when(userRepository.findByEmailAndDeletedAtIsNull("admin@msp.com")).thenReturn(Optional.of(u));
            ResponseEntity<Map<String, Object>> resp = authController.login(req);
            assertThat(resp.getBody()).containsKey("token");
        }

        @Test @DisplayName("returns mfaRequired when MFA is enabled")
        void mfaRequired() {
            LoginRequest req = new LoginRequest();
            req.setEmail("mfa@msp.com");
            req.setPassword("pass");
            AuthResponse authRes = AuthResponse.builder().token("jwt").role("ADMIN").build();
            when(authService.login(any(LoginRequest.class))).thenReturn(authRes);
            User u = new User(); u.setEmail("mfa@msp.com"); u.setMfaEnabled(true);
            when(userRepository.findByEmailAndDeletedAtIsNull("mfa@msp.com")).thenReturn(Optional.of(u));
            ResponseEntity<Map<String, Object>> resp = authController.login(req);
            assertThat(resp.getBody()).containsKey("mfaRequired");
        }
    }

    @Nested @DisplayName("GET /api/auth/me")
    class GetCurrentUserTests {

        @Test @DisplayName("returns user info for authenticated user")
        void returnsCurrentUser() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("admin@msp.com");
            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(ctx);
            User u = new User(); u.setEmail("admin@msp.com"); u.setFirstName("Admin");
            when(userRepository.findByEmailAndDeletedAtIsNull("admin@msp.com")).thenReturn(Optional.of(u));
            when(userRoleRepository.findByUserId(any())).thenReturn(List.of());
            Map<String, Object> result = authController.getCurrentUser();
            assertThat(result).containsEntry("email", "admin@msp.com");
        }
    }

    @Nested @DisplayName("POST /api/auth/forgot-password")
    class ForgotPasswordTests {

        @Test @DisplayName("returns success for anti-enumeration")
        void antiEnumeration() {
            when(authService.forgotPassword("none@test.com")).thenReturn(Map.of("message", "ok"));
            ResponseEntity<Map<String, Object>> resp =
                    authController.forgotPassword(Map.of("email", "none@test.com"));
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested @DisplayName("POST /api/auth/reset-password")
    class ResetPasswordTests {

        @Test @DisplayName("returns 400 for invalid reset token")
        void invalidToken() {
            doThrow(new RuntimeException("Invalid")).when(authService).resetPassword("bad", "Pass!");
            ResponseEntity<Map<String, Object>> resp =
                    authController.resetPassword(Map.of("token", "bad", "newPassword", "Pass!"));
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test @DisplayName("returns 200 for valid reset")
        void validReset() {
            doNothing().when(authService).resetPassword("good", "Pass!");
            ResponseEntity<Map<String, Object>> resp =
                    authController.resetPassword(Map.of("token", "good", "newPassword", "Pass!"));
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
