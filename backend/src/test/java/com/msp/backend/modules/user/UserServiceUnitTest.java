package com.msp.backend.modules.user;

import com.msp.backend.modules.role.Role;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.modules.role.RolePermissionRepository;
import com.msp.backend.modules.merchant.MerchantUserMappingRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for UserService.
 * Tests business logic in isolation using Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Unit Tests")
class UserServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private MerchantUserMappingRepository merchantUserMappingRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private UserService userService;

    private User merchantUser;
    private User adminUser;
    private Role merchantRole;
    private Role adminRole;
    private UserRole merchantUserRole;
    private UserRole adminUserRole;

    @BeforeEach
    void setUp() {
        merchantUser = new User();
        merchantUser.setUserId(1L);
        merchantUser.setEmail("merchant@test.com");
        merchantUser.setPassword("hashedPassword");
        merchantUser.setDisplayName("MerchantUser");
        merchantUser.setFirstName("John");
        merchantUser.setLastName("Doe");
        merchantUser.setStatus("ACTIVE");
        merchantUser.setCreatedAt(LocalDateTime.now());

        adminUser = new User();
        adminUser.setUserId(2L);
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword("hashedPassword");
        adminUser.setDisplayName("AdminUser");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setStatus("ACTIVE");

        merchantRole = new Role();
        merchantRole.setRoleId(1L);
        merchantRole.setRoleName("MERCHANT");
        merchantRole.setRoleType("BUSINESS");

        adminRole = new Role();
        adminRole.setRoleId(2L);
        adminRole.setRoleName("ADMIN");
        adminRole.setRoleType("SYSTEM");

        merchantUserRole = new UserRole();
        merchantUserRole.setUserId(1L);
        merchantUserRole.setRoleId(1L);

        adminUserRole = new UserRole();
        adminUserRole.setUserId(2L);
        adminUserRole.setRoleId(2L);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  getAllUsers
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsersTests {

        @Test
        @DisplayName("returns all non-deleted users with roles populated")
        void returnsListWithRoles() {
            when(userRepository.findByDeletedAtIsNull()).thenReturn(List.of(merchantUser));
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(merchantUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));

            List<User> result = userService.getAllUsers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRole()).isEqualTo("MERCHANT");
            assertThat(result.get(0).getEmail()).isEqualTo("merchant@test.com");
        }

        @Test
        @DisplayName("returns empty list when no active users exist")
        void returnsEmptyList() {
            when(userRepository.findByDeletedAtIsNull()).thenReturn(List.of());
            assertThat(userService.getAllUsers()).isEmpty();
        }

        @Test
        @DisplayName("returns multiple users with correct roles")
        void returnsMultipleUsersWithRoles() {
            when(userRepository.findByDeletedAtIsNull()).thenReturn(List.of(merchantUser, adminUser));
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(merchantUserRole));
            when(userRoleRepository.findByUserId(2L)).thenReturn(List.of(adminUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));
            when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));

            List<User> result = userService.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getRole()).isEqualTo("MERCHANT");
            assertThat(result.get(1).getRole()).isEqualTo("ADMIN");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  getUserById
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getUserById")
    class GetUserByIdTests {

        @Test
        @DisplayName("returns user with role populated when found")
        void returnsUserWithRole() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(merchantUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));

            User result = userService.getUserById(1L);

            assertThat(result.getEmail()).isEqualTo("merchant@test.com");
            assertThat(result.getRole()).isEqualTo("MERCHANT");
        }

        @Test
        @DisplayName("throws RuntimeException when user not found")
        void throwsWhenNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  createUser
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createUser")
    class CreateUserTests {

        @Test
        @DisplayName("creates user with hashed default password and ACTIVE status")
        void createsWithDefaults() {
            User input = new User();
            input.setEmail("new@test.com");
            input.setFirstName("New");
            input.setLastName("User");
            input.setDisplayName("NewUser");
            input.setRole("MERCHANT");

            when(userRepository.existsByEmailAndDeletedAtIsNull("new@test.com")).thenReturn(false);
            when(userRepository.existsByDisplayNameIgnoreCaseAndDeletedAtIsNull("NewUser")).thenReturn(false);
            when(passwordEncoder.encode("P@ssw0rd")).thenReturn("$2a$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(99L);
                return u;
            });
            when(roleRepository.findByRoleNameAndDeletedAtIsNull("MERCHANT")).thenReturn(Optional.of(merchantRole));
            when(userRoleRepository.save(any(UserRole.class))).thenReturn(merchantUserRole);

            User result = userService.createUser(input);

            assertThat(result.getPassword()).isEqualTo("$2a$hashed");
            assertThat(result.getStatus()).isEqualTo("ACTIVE");
            assertThat(result.getMustChangePassword()).isTrue();
            assertThat(result.getRole()).isEqualTo("MERCHANT");
        }

        @Test
        @DisplayName("hashes custom password when provided")
        void hashesCustomPassword() {
            User input = new User();
            input.setEmail("new@test.com");
            input.setFirstName("New");
            input.setLastName("User");
            input.setDisplayName("NewUser");
            input.setPassword("MySecret123!");
            input.setRole("MERCHANT");

            when(userRepository.existsByEmailAndDeletedAtIsNull("new@test.com")).thenReturn(false);
            when(userRepository.existsByDisplayNameIgnoreCaseAndDeletedAtIsNull("NewUser")).thenReturn(false);
            when(passwordEncoder.encode("MySecret123!")).thenReturn("$2a$custom");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(88L);
                return u;
            });
            when(roleRepository.findByRoleNameAndDeletedAtIsNull("MERCHANT")).thenReturn(Optional.of(merchantRole));
            when(userRoleRepository.save(any(UserRole.class))).thenReturn(merchantUserRole);

            User result = userService.createUser(input);

            assertThat(result.getPassword()).isEqualTo("$2a$custom");
            verify(passwordEncoder).encode("MySecret123!");
        }

        @Test
        @DisplayName("throws when email already exists")
        void throwsOnDuplicateEmail() {
            User input = new User();
            input.setEmail("merchant@test.com");
            input.setDisplayName("SomeUser");

            when(userRepository.existsByEmailAndDeletedAtIsNull("merchant@test.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(input))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email already exists");
        }

        @Test
        @DisplayName("throws when display name already taken")
        void throwsOnDuplicateDisplayName() {
            User input = new User();
            input.setEmail("new@test.com");
            input.setDisplayName("ExistingName");

            when(userRepository.existsByEmailAndDeletedAtIsNull("new@test.com")).thenReturn(false);
            when(userRepository.existsByDisplayNameIgnoreCaseAndDeletedAtIsNull("ExistingName")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(input))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already taken");
        }

        @Test
        @DisplayName("throws when no role provided")
        void throwsWhenNoRole() {
            User input = new User();
            input.setEmail("new@test.com");
            input.setFirstName("New");
            input.setLastName("User");
            input.setDisplayName("NewUser");

            when(userRepository.existsByEmailAndDeletedAtIsNull("new@test.com")).thenReturn(false);
            when(userRepository.existsByDisplayNameIgnoreCaseAndDeletedAtIsNull("NewUser")).thenReturn(false);

            assertThatThrownBy(() -> userService.createUser(input))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("role must be assigned");
        }

        @Test
        @DisplayName("gracefully handles missing role name during assignment")
        void handlesRoleNotFoundGracefully() {
            User input = new User();
            input.setEmail("new@test.com");
            input.setFirstName("New");
            input.setLastName("User");
            input.setDisplayName("NewUser");
            input.setRole("NONEXISTENT");

            when(userRepository.existsByEmailAndDeletedAtIsNull("new@test.com")).thenReturn(false);
            when(userRepository.existsByDisplayNameIgnoreCaseAndDeletedAtIsNull("NewUser")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(77L);
                return u;
            });
            when(roleRepository.findByRoleNameAndDeletedAtIsNull("NONEXISTENT")).thenReturn(Optional.empty());

            User result = userService.createUser(input);

            assertThat(result.getUserId()).isEqualTo(77L);
            assertThat(result.getRole()).isEqualTo("NONEXISTENT");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  updateUser
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTests {

        @Test
        @DisplayName("updates firstName, lastName, status")
        void updatesBasicFields() {
            User updates = new User();
            updates.setFirstName("Jane");
            updates.setLastName("Smith");
            updates.setStatus("INACTIVE");

            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(merchantUser);
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(merchantUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));
            doNothing().when(entityManager).detach(any());

            userService.updateUser(1L, updates);

            assertThat(merchantUser.getFirstName()).isEqualTo("Jane");
            assertThat(merchantUser.getLastName()).isEqualTo("Smith");
            assertThat(merchantUser.getStatus()).isEqualTo("INACTIVE");
        }

        @Test
        @DisplayName("updates displayName and checks uniqueness")
        void updatesDisplayName() {
            User updates = new User();
            updates.setDisplayName("NewDisplayName");

            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(userRepository.existsByDisplayNameIgnoreCaseAndUserIdNotAndDeletedAtIsNull("NewDisplayName", 1L)).thenReturn(false);
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(merchantUser);
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(merchantUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));
            doNothing().when(entityManager).detach(any());

            userService.updateUser(1L, updates);

            assertThat(merchantUser.getDisplayName()).isEqualTo("NewDisplayName");
        }

        @Test
        @DisplayName("throws when display name taken by another user")
        void throwsOnDuplicateDisplayName() {
            User updates = new User();
            updates.setDisplayName("TakenName");

            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(userRepository.existsByDisplayNameIgnoreCaseAndUserIdNotAndDeletedAtIsNull("TakenName", 1L)).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, updates))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already taken");
        }

        @Test
        @DisplayName("does not overwrite fields with blank values")
        void doesNotOverwriteBlankFields() {
            merchantUser.setFirstName("Original");
            User updates = new User();
            updates.setFirstName("");

            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(merchantUser);
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(merchantUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));
            doNothing().when(entityManager).detach(any());

            userService.updateUser(1L, updates);

            assertThat(merchantUser.getFirstName()).isEqualTo("Original");
        }

        @Test
        @DisplayName("throws when user not found")
        void throwsWhenNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(999L, new User()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("clears password from response")
        void clearsPasswordFromResponse() {
            User updates = new User();
            updates.setFirstName("Updated");

            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(userRepository.saveAndFlush(any(User.class))).thenReturn(merchantUser);
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(merchantUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));
            doNothing().when(entityManager).detach(any());

            User result = userService.updateUser(1L, updates);

            assertThat(result.getPassword()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  deleteUser
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteUser")
    class DeleteUserTests {

        @Test
        @DisplayName("soft-deletes user by setting deletedAt")
        void softDeletesUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(merchantUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));
            when(userRepository.save(any(User.class))).thenReturn(merchantUser);

            userService.deleteUser(1L);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("cleans up user roles and merchant mappings on delete")
        void cleansUpJunctionTables() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(merchantUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));
            when(userRepository.save(any(User.class))).thenReturn(merchantUser);

            userService.deleteUser(1L);

            verify(userRoleRepository).deleteByUserId(1L);
            verify(merchantUserMappingRepository).deleteByUserId(1L);
        }

        @Test
        @DisplayName("throws when user not found")
        void throwsWhenNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("prevents deleting the last SYSTEM-role (bank) user")
        void preventsLastBankUserDeletion() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
            when(userRoleRepository.findByUserId(2L)).thenReturn(List.of(adminUserRole));
            when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));
            when(userRepository.findByDeletedAtIsNull()).thenReturn(List.of(adminUser));

            assertThatThrownBy(() -> userService.deleteUser(2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot delete the last bank user");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  resetPassword
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("encodes and saves new password")
        void encodesAndSaves() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(merchantUser));
            when(passwordEncoder.encode("NewPass123!")).thenReturn("$2a$newHashed");
            when(userRepository.save(any(User.class))).thenReturn(merchantUser);

            userService.resetPassword(1L, "NewPass123!");

            assertThat(merchantUser.getPassword()).isEqualTo("$2a$newHashed");
            verify(userRepository).save(merchantUser);
        }

        @Test
        @DisplayName("throws when user not found")
        void throwsWhenNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.resetPassword(999L, "pass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  populateRole
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("populateRole")
    class PopulateRoleTests {

        @Test
        @DisplayName("sets ADMIN for user with SYSTEM-type role")
        void setsAdminForSystemRole() {
            when(userRoleRepository.findByUserId(2L)).thenReturn(List.of(adminUserRole));
            when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));

            userService.populateRole(adminUser);

            assertThat(adminUser.getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("sets MERCHANT for user with BUSINESS-type role")
        void setsMerchantForBusinessRole() {
            when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(merchantUserRole));
            when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));

            userService.populateRole(merchantUser);

            assertThat(merchantUser.getRole()).isEqualTo("MERCHANT");
        }

        @Test
        @DisplayName("leaves role null when user has no roles")
        void noRolesLeavesNull() {
            User noRoleUser = new User();
            noRoleUser.setUserId(99L);
            when(userRoleRepository.findByUserId(99L)).thenReturn(List.of());

            userService.populateRole(noRoleUser);

            assertThat(noRoleUser.getRole()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  autoInactivateInactiveUsers
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("autoInactivateInactiveUsers")
    class AutoInactivateTests {

        @Test
        @DisplayName("inactivates user who never logged in and created >30 days ago")
        void inactivatesNeverLoggedIn() {
            User oldUser = new User();
            oldUser.setUserId(10L);
            oldUser.setStatus("ACTIVE");
            oldUser.setCreatedAt(LocalDateTime.now().minusDays(31));
            oldUser.setLastLoginAt(null);

            when(userRepository.findByDeletedAtIsNullAndStatus("ACTIVE")).thenReturn(List.of(oldUser));
            when(userRepository.save(any(User.class))).thenReturn(oldUser);

            userService.autoInactivateInactiveUsers();

            assertThat(oldUser.getStatus()).isEqualTo("INACTIVE");
            verify(userRepository).save(oldUser);
        }

        @Test
        @DisplayName("inactivates user whose last login was >30 days ago")
        void inactivatesOldLogin() {
            User staleUser = new User();
            staleUser.setUserId(11L);
            staleUser.setStatus("ACTIVE");
            staleUser.setCreatedAt(LocalDateTime.now().minusDays(60));
            staleUser.setLastLoginAt(LocalDateTime.now().minusDays(35));

            when(userRepository.findByDeletedAtIsNullAndStatus("ACTIVE")).thenReturn(List.of(staleUser));
            when(userRepository.save(any(User.class))).thenReturn(staleUser);

            userService.autoInactivateInactiveUsers();

            assertThat(staleUser.getStatus()).isEqualTo("INACTIVE");
        }

        @Test
        @DisplayName("does not inactivate recently active user")
        void doesNotInactivateRecentUser() {
            User recentUser = new User();
            recentUser.setUserId(12L);
            recentUser.setStatus("ACTIVE");
            recentUser.setCreatedAt(LocalDateTime.now().minusDays(5));
            recentUser.setLastLoginAt(LocalDateTime.now().minusDays(1));

            when(userRepository.findByDeletedAtIsNullAndStatus("ACTIVE")).thenReturn(List.of(recentUser));

            userService.autoInactivateInactiveUsers();

            assertThat(recentUser.getStatus()).isEqualTo("ACTIVE");
            verify(userRepository, never()).save(any());
        }
    }
}
