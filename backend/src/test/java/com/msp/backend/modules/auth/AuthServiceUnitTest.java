package com.msp.backend.modules.auth;

import com.msp.backend.modules.auth.dto.AuthResponse;
import com.msp.backend.modules.auth.dto.LoginRequest;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for AuthService.
 * Tests authentication, forgot-password and reset-password flows.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Unit Tests")
class AuthServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setUserId(1L);
        activeUser.setEmail("user@test.com");
        activeUser.setPassword("hashedPassword");
        activeUser.setFirstName("John");
        activeUser.setLastName("Doe");
        activeUser.setDisplayName("JohnDoe");
        activeUser.setStatus("ACTIVE");
        activeUser.setRole("MERCHANT");

        loginRequest = new LoginRequest();
        loginRequest.setIdentifier("user@test.com");
        loginRequest.setPassword("password123");
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("successful login returns JWT token and role")
        void successfulLogin() {
            when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            doNothing().when(userService).populateRole(activeUser);
            when(jwtService.generateToken("user@test.com", "MERCHANT")).thenReturn("jwt-token-abc");
            when(userRepository.save(any())).thenReturn(activeUser);

            AuthResponse response = authService.login(loginRequest);

            assertThat(response.getToken()).isEqualTo("jwt-token-abc");
            assertThat(response.getRole()).isEqualTo("MERCHANT");
        }

        @Test
        @DisplayName("login via display name when email not found")
        void loginViaDisplayName() {
            loginRequest.setIdentifier("JohnDoe");
            when(userRepository.findByEmailAndDeletedAtIsNull("JohnDoe")).thenReturn(Optional.empty());
            when(userRepository.findByDisplayNameIgnoreCaseAndDeletedAtIsNull("JohnDoe")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            doNothing().when(userService).populateRole(activeUser);
            when(jwtService.generateToken("user@test.com", "MERCHANT")).thenReturn("jwt-token");
            when(userRepository.save(any())).thenReturn(activeUser);

            AuthResponse response = authService.login(loginRequest);

            assertThat(response.getToken()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("throws when identifier is blank")
        void throwsOnBlankIdentifier() {
            loginRequest.setIdentifier("");
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("throws when user not found")
        void throwsOnUserNotFound() {
            when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.empty());
            when(userRepository.findByDisplayNameIgnoreCaseAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("throws when user is INACTIVE")
        void throwsOnInactiveUser() {
            activeUser.setStatus("INACTIVE");
            when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("inactive");
        }

        @Test
        @DisplayName("throws when user is SUSPENDED")
        void throwsOnSuspendedUser() {
            activeUser.setStatus("SUSPENDED");
            when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("suspended");
        }

        @Test
        @DisplayName("throws when password is wrong")
        void throwsOnWrongPassword() {
            when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("throws when user has no role assigned")
        void throwsOnNoRole() {
            activeUser.setRole(null);
            when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            doNothing().when(userService).populateRole(activeUser);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no role");
        }

        @Test
        @DisplayName("updates lastLoginAt on successful login")
        void updatesLastLoginAt() {
            when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
            doNothing().when(userService).populateRole(activeUser);
            when(jwtService.generateToken(anyString(), anyString())).thenReturn("token");
            when(userRepository.save(any())).thenReturn(activeUser);

            authService.login(loginRequest);

            verify(userRepository).save(argThat(u -> u.getLastLoginAt() != null));
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("returns generic message for unknown email (prevents enumeration)")
        void unknownEmailReturnsGenericMessage() {
            when(userRepository.findByEmailAndDeletedAtIsNull("unknown@test.com")).thenReturn(Optional.empty());

            Map<String, Object> result = authService.forgotPassword("unknown@test.com");

            assertThat(result.get("message").toString()).contains("If that email");
        }

        @Test
        @DisplayName("generates reset token for valid active user")
        void generatesResetToken() {
            when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));
            when(userRepository.save(any())).thenReturn(activeUser);

            Map<String, Object> result = authService.forgotPassword("user@test.com");

            assertThat(result).containsKey("resetToken");
            verify(userRepository).save(argThat(u -> u.getResetToken() != null && u.getResetTokenExpiry() != null));
        }

        @Test
        @DisplayName("does not generate token for inactive user")
        void noTokenForInactiveUser() {
            activeUser.setStatus("INACTIVE");
            when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));

            Map<String, Object> result = authService.forgotPassword("user@test.com");

            assertThat(result).doesNotContainKey("resetToken");
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("throws on blank token")
        void throwsOnBlankToken() {
            assertThatThrownBy(() -> authService.resetPassword("", "newpass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid or expired");
        }

        @Test
        @DisplayName("throws on expired token")
        void throwsOnExpiredToken() {
            activeUser.setResetToken("expired-token");
            activeUser.setResetTokenExpiry(LocalDateTime.now().minusHours(2));
            when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(activeUser));
            when(userRepository.save(any())).thenReturn(activeUser);

            assertThatThrownBy(() -> authService.resetPassword("expired-token", "newpass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("successfully resets password with valid token")
        void successfulReset() {
            activeUser.setResetToken("valid-token");
            activeUser.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(activeUser));
            doNothing().when(userService).resetPassword(anyLong(), anyString());
            when(userRepository.save(any())).thenReturn(activeUser);

            assertThatNoException().isThrownBy(() -> authService.resetPassword("valid-token", "newpass123"));
            verify(userService).resetPassword(1L, "newpass123");
        }

        @Test
        @DisplayName("clears reset token after successful reset")
        void clearsTokenAfterReset() {
            activeUser.setResetToken("valid-token");
            activeUser.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(activeUser));
            doNothing().when(userService).resetPassword(anyLong(), anyString());
            when(userRepository.save(any())).thenReturn(activeUser);

            authService.resetPassword("valid-token", "newpass");

            verify(userRepository).save(argThat(u -> u.getResetToken() == null && u.getResetTokenExpiry() == null));
        }
    }
}
