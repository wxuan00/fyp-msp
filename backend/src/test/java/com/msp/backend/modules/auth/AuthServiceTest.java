package com.msp.backend.modules.auth;

import com.msp.backend.modules.auth.dto.AuthResponse;
import com.msp.backend.modules.auth.dto.LoginRequest;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

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
        activeUser.setStatus("ACTIVE");
        activeUser.setRole("MERCHANT");

        loginRequest = new LoginRequest();
        loginRequest.setIdentifier("user@test.com");
        loginRequest.setPassword("password123");
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: success returns token and role")
    void login_success() {
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        doNothing().when(userService).populateRole(activeUser);
        when(jwtService.generateToken("user@test.com", "MERCHANT")).thenReturn("jwt-token");
        when(userRepository.save(any())).thenReturn(activeUser);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRole()).isEqualTo("MERCHANT");
    }

    @Test
    @DisplayName("login: blank identifier throws exception")
    void login_blankIdentifier_throws() {
        loginRequest.setIdentifier("");
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("login: user not found throws invalid credentials")
    void login_userNotFound_throws() {
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByDisplayNameIgnoreCaseAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("login: INACTIVE user throws account inactive")
    void login_inactiveUser_throws() {
        activeUser.setStatus("INACTIVE");
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("login: SUSPENDED user throws account suspended")
    void login_suspendedUser_throws() {
        activeUser.setStatus("SUSPENDED");
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("suspended");
    }

    @Test
    @DisplayName("login: wrong password throws invalid credentials")
    void login_wrongPassword_throws() {
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("login: user with no role throws exception")
    void login_noRole_throws() {
        activeUser.setRole(null);
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        doNothing().when(userService).populateRole(activeUser);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no role");
    }

    // ─── forgotPassword ───────────────────────────────────────────────────────

    @Test
    @DisplayName("forgotPassword: always returns success message even for unknown email")
    void forgotPassword_unknownEmail_returnsMessage() {
        when(userRepository.findByEmailAndDeletedAtIsNull("noone@test.com")).thenReturn(Optional.empty());

        Map<String, Object> result = authService.forgotPassword("noone@test.com");

        assertThat(result.get("message").toString()).contains("If that email");
    }

    @Test
    @DisplayName("forgotPassword: valid active user gets reset token")
    void forgotPassword_validUser_setsToken() {
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenReturn(activeUser);

        Map<String, Object> result = authService.forgotPassword("user@test.com");

        assertThat(result).containsKey("resetToken");
        verify(userRepository).save(argThat(u -> u.getResetToken() != null));
    }

    @Test
    @DisplayName("forgotPassword: inactive user returns message but no token")
    void forgotPassword_inactiveUser_noToken() {
        activeUser.setStatus("INACTIVE");
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(Optional.of(activeUser));

        Map<String, Object> result = authService.forgotPassword("user@test.com");

        assertThat(result).doesNotContainKey("resetToken");
    }

    // ─── resetPassword ────────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword: blank token throws exception")
    void resetPassword_blankToken_throws() {
        assertThatThrownBy(() -> authService.resetPassword("", "newpass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    @DisplayName("resetPassword: expired token throws exception")
    void resetPassword_expiredToken_throws() {
        activeUser.setResetToken("expired-token");
        activeUser.setResetTokenExpiry(LocalDateTime.now().minusHours(2));
        when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenReturn(activeUser);

        assertThatThrownBy(() -> authService.resetPassword("expired-token", "newpass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("resetPassword: valid token resets password and clears token")
    void resetPassword_validToken_resetsPassword() {
        activeUser.setResetToken("valid-token");
        activeUser.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(activeUser));
        doNothing().when(userService).resetPassword(anyLong(), anyString());
        when(userRepository.save(any())).thenReturn(activeUser);

        assertThatNoException().isThrownBy(() -> authService.resetPassword("valid-token", "newpass123"));
        verify(userService).resetPassword(1L, "newpass123");
    }
}
