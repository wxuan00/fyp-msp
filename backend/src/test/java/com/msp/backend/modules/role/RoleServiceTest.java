package com.msp.backend.modules.role;

import com.msp.backend.modules.role.Role;
import com.msp.backend.modules.role.RolePermissionRepository;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.modules.role.RoleService;
import com.msp.backend.modules.user.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Unit Tests")
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private UserRoleRepository userRoleRepository;

    @InjectMocks
    private RoleService roleService;

    private Role merchantRole;
    private Role adminRole;
    private Role customRole;

    @BeforeEach
    void setUp() {
        merchantRole = new Role();
        merchantRole.setRoleId(1L);
        merchantRole.setRoleName("MERCHANT");
        merchantRole.setRoleType("BUSINESS");
        merchantRole.setDescription("Merchant role");

        adminRole = new Role();
        adminRole.setRoleId(2L);
        adminRole.setRoleName("ADMIN");
        adminRole.setRoleType("SYSTEM");
        adminRole.setDescription("Admin role");

        customRole = new Role();
        customRole.setRoleId(3L);
        customRole.setRoleName("CUSTOM_ROLE");
        customRole.setRoleType("BUSINESS");
        customRole.setDescription("Custom business role");
    }

    // ─── getAllRoles ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllRoles: returns all roles from repository")
    void getAllRoles_returnsAll() {
        when(roleRepository.findByDeletedAtIsNull()).thenReturn(List.of(merchantRole, adminRole, customRole));
        List<Role> result = roleService.getAllRoles();
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("getAllRoles: returns empty list when no roles")
    void getAllRoles_empty() {
        when(roleRepository.findByDeletedAtIsNull()).thenReturn(List.of());
        assertThat(roleService.getAllRoles()).isEmpty();
    }

    // ─── getRoleById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRoleById: returns role when found")
    void getRoleById_found() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));
        Role result = roleService.getRoleById(1L);
        assertThat(result.getRoleName()).isEqualTo("MERCHANT");
    }

    @Test
    @DisplayName("getRoleById: throws when not found")
    void getRoleById_notFound_throws() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roleService.getRoleById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Role not found");
    }

    // ─── createRole ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRole: saves and returns new role")
    void createRole_success() {
        when(roleRepository.findByRoleNameAndDeletedAtIsNull("CUSTOM_ROLE")).thenReturn(Optional.empty());
        when(roleRepository.save(any())).thenReturn(customRole);

        Role result = roleService.createRole(customRole);

        assertThat(result.getRoleName()).isEqualTo("CUSTOM_ROLE");
        verify(roleRepository).save(customRole);
    }

    @Test
    @DisplayName("createRole: throws when role name already exists")
    void createRole_duplicateName_throws() {
        when(roleRepository.findByRoleNameAndDeletedAtIsNull("CUSTOM_ROLE")).thenReturn(Optional.of(customRole));
        assertThatThrownBy(() -> roleService.createRole(customRole))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    // ─── updateRole ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateRole: updates role fields and saves")
    void updateRole_success() {
        Role updated = new Role();
        updated.setRoleName("CUSTOM_UPDATED");
        updated.setDescription("Updated description");
        updated.setRoleType("BUSINESS");

        when(roleRepository.findById(3L)).thenReturn(Optional.of(customRole));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Role result = roleService.updateRole(3L, updated);

        assertThat(result.getRoleName()).isEqualTo("CUSTOM_UPDATED");
        assertThat(result.getDescription()).isEqualTo("Updated description");
    }

    @Test
    @DisplayName("updateRole: throws when role not found")
    void updateRole_notFound_throws() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roleService.updateRole(99L, customRole))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Role not found");
    }

    // ─── deleteRole ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteRole: deletes non-system role")
    void deleteRole_success() {
        when(roleRepository.findById(3L)).thenReturn(Optional.of(customRole));
        doNothing().when(rolePermissionRepository).deleteByRoleId(3L);
        doNothing().when(userRoleRepository).deleteByRoleId(3L);
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        assertThatNoException().isThrownBy(() -> roleService.deleteRole(3L));
        verify(roleRepository).save(argThat(r -> r.getDeletedAt() != null));
    }

    @Test
    @DisplayName("deleteRole: throws when deleting ADMIN system role")
    void deleteRole_adminRole_throws() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));
        assertThatThrownBy(() -> roleService.deleteRole(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete system role");
    }

    @Test
    @DisplayName("deleteRole: throws when deleting MERCHANT system role")
    void deleteRole_merchantRole_throws() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(merchantRole));
        assertThatThrownBy(() -> roleService.deleteRole(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete system role");
    }

    @Test
    @DisplayName("deleteRole: throws when role not found")
    void deleteRole_notFound_throws() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roleService.deleteRole(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Role not found");
    }
}
