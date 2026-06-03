package com.msp.backend.modules.user;

import com.msp.backend.modules.role.Role;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.modules.role.RolePermissionRepository;
import com.msp.backend.modules.merchant.MerchantUserMappingRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private MerchantUserMappingRepository merchantUserMappingRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private User adminUser;
    private Role merchantRole;
    private Role adminRole;
    private UserRole sampleUserRole;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(1L);
        sampleUser.setEmail("merchant@test.com");
        sampleUser.setPassword("hashedPassword");
        sampleUser.setFirstName("John");
        sampleUser.setLastName("Doe");
        sampleUser.setStatus("ACTIVE");
        sampleUser.setCreatedAt(LocalDateTime.now());

        adminUser = new User();
        adminUser.setUserId(2L);
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword("hashedPassword");
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

        sampleUserRole = new UserRole();
        sampleUserRole.setUserId(1L);
        sampleUserRole.setRoleId(1L);
    }

    // ─── getAllUsers ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsers: returns all non-deleted users with roles populated")
    void getAllUsers_returnsListWithRoles() {
        when(userRepository.findByDeletedAtIsNull()).thenReturn(List.of(sampleUser));
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(sampleUserRole));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo("MERCHANT");
    }

    @Test
    @DisplayName("getAllUsers: returns empty list when no users")
    void getAllUsers_emptyList() {
        when(userRepository.findByDeletedAtIsNull()).thenReturn(List.of());
        assertThat(userService.getAllUsers()).isEmpty();
    }

    // ─── getUserById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById: returns user with role populated")
    void getUserById_found_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(sampleUserRole));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));

        User result = userService.getUserById(1L);

        assertThat(result.getEmail()).isEqualTo("merchant@test.com");
        assertThat(result.getRole()).isEqualTo("MERCHANT");
    }

    @Test
    @DisplayName("getUserById: throws RuntimeException when not found")
    void getUserById_notFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─── createUser ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createUser: creates user with provided role and hashed password")
    void createUser_withRole_createsSuccessfully() {
        User input = new User();
        input.setEmail("new@test.com");
        input.setFirstName("New");
        input.setLastName("User");
        input.setRole("MERCHANT");

        when(userRepository.existsByEmailAndDeletedAtIsNull("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(99L);
            return u;
        });
        when(roleRepository.findByRoleNameAndDeletedAtIsNull("MERCHANT")).thenReturn(Optional.of(merchantRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(sampleUserRole);

        User result = userService.createUser(input);

        assertThat(result.getPassword()).isEqualTo("$2a$hashed");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getMustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("createUser: throws when no role provided")
    void createUser_noRole_throwsException() {
        User input = new User();
        input.setEmail("new@test.com");
        input.setFirstName("New");
        input.setLastName("User");

        when(userRepository.existsByEmailAndDeletedAtIsNull("new@test.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.createUser(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("role must be assigned");
    }

    @Test
    @DisplayName("createUser: throws when email already exists (non-deleted)")
    void createUser_duplicateEmail_throwsException() {
        User input = new User();
        input.setEmail("merchant@test.com");

        when(userRepository.existsByEmailAndDeletedAtIsNull("merchant@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("createUser: uses provided password when not blank")
    void createUser_providedPassword_isHashed() {
        User input = new User();
        input.setEmail("new2@test.com");
        input.setFirstName("Test");
        input.setLastName("User");
        input.setPassword("MySecret123");
        input.setRole("MERCHANT");

        when(userRepository.existsByEmailAndDeletedAtIsNull("new2@test.com")).thenReturn(false);
        when(passwordEncoder.encode("MySecret123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(88L);
            return u;
        });
        when(roleRepository.findByRoleNameAndDeletedAtIsNull("MERCHANT")).thenReturn(Optional.of(merchantRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(sampleUserRole);

        User result = userService.createUser(input);

        assertThat(result.getPassword()).isEqualTo("$2a$encoded");
        verify(passwordEncoder).encode("MySecret123");
    }

    // ─── deleteUser ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUser: soft-deletes user by setting deletedAt")
    void deleteUser_softDeletes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(sampleUserRole));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        userService.deleteUser(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("deleteUser: throws when user not found")
    void deleteUser_notFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("deleteUser: throws when deleting the last SYSTEM-role user")
    void deleteUser_lastBankUser_throwsException() {
        UserRole adminUserRole = new UserRole();
        adminUserRole.setUserId(2L);
        adminUserRole.setRoleId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRoleRepository.findByUserId(2L)).thenReturn(List.of(adminUserRole));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));
        when(userRepository.findByDeletedAtIsNull()).thenReturn(List.of(adminUser));

        assertThatThrownBy(() -> userService.deleteUser(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete the last bank user");
    }

    // ─── updateUser ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUser: updates firstName, lastName, status")
    void updateUser_updatesBasicFields() {
        User updates = new User();
        updates.setFirstName("Jane");
        updates.setLastName("Smith");
        updates.setStatus("INACTIVE");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(sampleUser);
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(sampleUserRole));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));
        doNothing().when(entityManager).detach(any());

        userService.updateUser(1L, updates);

        assertThat(sampleUser.getFirstName()).isEqualTo("Jane");
        assertThat(sampleUser.getLastName()).isEqualTo("Smith");
        assertThat(sampleUser.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("updateUser: throws when user not found")
    void updateUser_notFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(999L, new User()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─── resetPassword ───────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword: encodes and saves new password")
    void resetPassword_encodesAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("NewPass123!")).thenReturn("$2a$newHashed");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        userService.resetPassword(1L, "NewPass123!");

        assertThat(sampleUser.getPassword()).isEqualTo("$2a$newHashed");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("resetPassword: throws when user not found")
    void resetPassword_notFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.resetPassword(999L, "pass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─── populateRole ────────────────────────────────────────────────────────

    @Test
    @DisplayName("populateRole: sets MERCHANT role for BUSINESS-type role")
    void populateRole_businessRole_setsMerchant() {
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of(sampleUserRole));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));

        userService.populateRole(sampleUser);

        assertThat(sampleUser.getRole()).isEqualTo("MERCHANT");
    }

    @Test
    @DisplayName("populateRole: sets ADMIN for SYSTEM-type role")
    void populateRole_systemRole_setsAdmin() {
        UserRole adminUserRole = new UserRole();
        adminUserRole.setUserId(2L);
        adminUserRole.setRoleId(2L);

        when(userRoleRepository.findByUserId(2L)).thenReturn(List.of(adminUserRole));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));

        userService.populateRole(adminUser);

        assertThat(adminUser.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("populateRole: does nothing when user has no roles")
    void populateRole_noRoles_doesNothing() {
        when(userRoleRepository.findByUserId(1L)).thenReturn(List.of());

        userService.populateRole(sampleUser);

        assertThat(sampleUser.getRole()).isNull();
    }
}
