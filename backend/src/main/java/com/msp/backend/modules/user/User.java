package com.msp.backend.modules.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt hashed (not in ERD but required for auth)

    @Column(name = "display_name", nullable = false)
    @NotBlank(message = "Display name is required")
    private String displayName;

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    private String status;

    // --- Auth fields (not in ERD but required for functionality) ---

    @Column(name = "is_mfa_enabled")
    @JsonProperty("mfaEnabled")
    private boolean mfaEnabled = false;

    @Column(name = "secret_key")
    private String secretKey; // For Google Authenticator 2FA

    @Column(name = "must_change_password", columnDefinition = "boolean default true")
    private Boolean mustChangePassword = true; // true for new users; cleared after first password change

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    // Transient field: populated from UserRoles junction table
    @Transient
    private String role;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastModifiedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedAt = LocalDateTime.now();
    }
}
