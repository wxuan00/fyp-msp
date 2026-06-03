package com.msp.backend.modules.user;

import com.msp.backend.modules.role.Role;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.modules.role.RolePermissionRepository;
import com.msp.backend.modules.merchant.MerchantUserMappingRepository;
import com.msp.backend.util.AuditHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final MerchantUserMappingRepository merchantUserMappingRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public List<User> getAllUsers() {
        List<User> users = userRepository.findByDeletedAtIsNull();
        users.forEach(this::populateRole);
        return users;
    }

    public User getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        populateRole(user);
        return user;
    }

    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(user.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            if (userRepository.existsByDisplayNameIgnoreCaseAndDeletedAtIsNull(user.getDisplayName().trim())) {
                throw new RuntimeException("Display name '" + user.getDisplayName().trim() + "' is already taken.");
            }
            user.setDisplayName(user.getDisplayName().trim());
        }

        String roleName = user.getRole();
        if (roleName == null || roleName.isBlank()) {
            throw new RuntimeException("A role must be assigned to the user.");
        }
        if (user.getStatus() == null) user.setStatus("ACTIVE");

        String rawPassword = (user.getPassword() == null || user.getPassword().isBlank())
                ? "P@ssw0rd"
                : user.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setCreatedBy(AuditHelper.currentUser());
        user.setLastModifiedBy(AuditHelper.currentUser());
        user.setMustChangePassword(true); // force password change on first login
        user.setRole(null);
        User saved = userRepository.save(user);

        // Try to assign the initial role by name; if no role exists with that exact name
        // (e.g. the base "MERCHANT" or "ADMIN" role was renamed), skip — the frontend will
        // call syncRoles immediately after with the correct roleIds.
        try {
            assignRole(saved.getUserId(), roleName, "SYSTEM");
        } catch (RuntimeException ignored) {
            // Role not found by name — will be set via syncRoles call from frontend
        }
        saved.setRole(roleName);
        return saved;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        populateRole(user);

        // Check if user is a bank (SYSTEM-role) user — protect against deleting last bank user
        boolean isBankUser = userRoleRepository.findByUserId(user.getUserId()).stream()
                .anyMatch(ur -> roleRepository.findById(ur.getRoleId())
                        .map(r -> "SYSTEM".equalsIgnoreCase(r.getRoleType())).orElse(false));

        if (isBankUser) {
            long bankUserCount = userRepository.findByDeletedAtIsNull().stream()
                    .filter(u -> userRoleRepository.findByUserId(u.getUserId()).stream()
                            .anyMatch(ur -> roleRepository.findById(ur.getRoleId())
                                    .map(r -> "SYSTEM".equalsIgnoreCase(r.getRoleType())).orElse(false)))
                    .count();
            if (bankUserCount <= 1) {
                throw new RuntimeException("CRITICAL: Cannot delete the last bank user!");
            }
        }

        user.setDeletedAt(java.time.LocalDateTime.now());
        user.setLastModifiedBy(AuditHelper.currentUser());
        user.setLastModifiedAt(java.time.LocalDateTime.now());
        // Clean up junction tables
        userRoleRepository.deleteByUserId(id);
        merchantUserMappingRepository.deleteByUserId(id);
        userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User updated) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only overwrite if value is non-null AND non-blank (blank = "no change sent")
        if (updated.getFirstName() != null && !updated.getFirstName().isBlank())
            user.setFirstName(updated.getFirstName());
        if (updated.getLastName() != null && !updated.getLastName().isBlank())
            user.setLastName(updated.getLastName());
        if (updated.getDisplayName() != null) {
            String newDisplay = updated.getDisplayName().isBlank() ? null : updated.getDisplayName().trim();
            if (newDisplay != null && userRepository.existsByDisplayNameIgnoreCaseAndUserIdNotAndDeletedAtIsNull(newDisplay, user.getUserId())) {
                throw new RuntimeException("Display name '" + newDisplay + "' is already taken.");
            }
            user.setDisplayName(newDisplay);
        }
        if (updated.getContactNumber() != null)
            user.setContactNumber(updated.getContactNumber().isBlank() ? null : updated.getContactNumber());
        if (updated.getStatus() != null && !updated.getStatus().isBlank())
            user.setStatus(updated.getStatus());

        user.setLastModifiedBy(AuditHelper.currentUser());
        user.setLastModifiedAt(java.time.LocalDateTime.now());
        // password is never updated through this method
        // role changes go through PUT /{id}/roles (syncRoles) only

        userRepository.saveAndFlush(user);
        entityManager.detach(user); // detach after flush so no dirty-check on response mutations
        populateRole(user);
        user.setPassword(null); // clear from response only
        return user;
    }

    public void populateRole(User user) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getUserId());
        if (userRoles.isEmpty()) return;

        // Any SYSTEM-type role → mark user as "ADMIN" so frontend isAdmin() works correctly.
        boolean hasSystemRole = userRoles.stream()
                .anyMatch(ur -> roleRepository.findById(ur.getRoleId())
                        .map(r -> "SYSTEM".equalsIgnoreCase(r.getRoleType())).orElse(false));

        if (hasSystemRole) {
            user.setRole("ADMIN");
            return;
        }

        // Merchant users: prefer MERCHANT base role name, else first role
        for (UserRole ur : userRoles) {
            roleRepository.findById(ur.getRoleId()).ifPresent(role -> {
                if ("MERCHANT".equalsIgnoreCase(role.getRoleName())) {
                    user.setRole(role.getRoleName());
                }
            });
        }
        if (user.getRole() == null) {
            roleRepository.findById(userRoles.get(0).getRoleId())
                    .ifPresent(role -> user.setRole(role.getRoleName()));
        }
    }

    private void assignRole(Long userId, String roleName, String generatedBy) {
        Role role = roleRepository.findByRoleNameAndDeletedAtIsNull(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getRoleId());
        userRole.setCreatedBy(generatedBy);
        userRoleRepository.save(userRole);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastModifiedBy(AuditHelper.currentUser());
        user.setLastModifiedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Runs daily at 02:00 AM. Auto-inactivates users who have never logged in AND
     * whose account was created more than 30 days ago, OR users whose last login
     * was more than 30 days ago.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoInactivateInactiveUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<User> candidates = userRepository.findByDeletedAtIsNullAndStatus("ACTIVE");
        for (User user : candidates) {
            boolean neverLoggedIn = user.getLastLoginAt() == null
                    && user.getCreatedAt() != null
                    && user.getCreatedAt().isBefore(cutoff);
            boolean inactiveTooLong = user.getLastLoginAt() != null
                    && user.getLastLoginAt().isBefore(cutoff);
            if (neverLoggedIn || inactiveTooLong) {
                user.setStatus("INACTIVE");
                user.setLastModifiedBy("SYSTEM");
                user.setLastModifiedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        }
    }
}
