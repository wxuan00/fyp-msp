package com.msp.backend.modules.role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.msp.backend.modules.user.UserRoleRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for RoleService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService — Unit Tests")
class RoleServiceUnitTest {

    @Mock private RoleRepository roleRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private UserRoleRepository userRoleRepository;

    @InjectMocks
    private RoleService roleService;

    private Role sampleRole;

    @BeforeEach
    void setUp() {
        sampleRole = new Role();
        sampleRole.setRoleId(1L);
        sampleRole.setRoleName("TESTER");
        sampleRole.setDescription("Tester role");
        sampleRole.setRoleType("CUSTOM");
    }

    @Nested
    @DisplayName("getAllRoles")
    class GetAllRolesTests {

        @Test
        @DisplayName("returns only non-deleted roles")
        void returnsNonDeleted() {
            when(roleRepository.findByDeletedAtIsNull()).thenReturn(List.of(sampleRole));
            List<Role> result = roleService.getAllRoles();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoleName()).isEqualTo("TESTER");
        }
    }

    @Nested
    @DisplayName("getRoleById")
    class GetRoleByIdTests {

        @Test
        @DisplayName("returns role when found")
        void returnsRole() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
            Role result = roleService.getRoleById(1L);
            assertThat(result.getRoleName()).isEqualTo("TESTER");
        }

        @Test
        @DisplayName("throws when not found")
        void throwsWhenNotFound() {
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> roleService.getRoleById(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("createRole")
    class CreateRoleTests {

        @Test
        @DisplayName("creates role with audit fields")
        void createsRole() {
            when(roleRepository.findByRoleNameAndDeletedAtIsNull("TESTER")).thenReturn(Optional.empty());
            when(roleRepository.save(any(Role.class))).thenReturn(sampleRole);

            Role result = roleService.createRole(sampleRole);
            assertThat(result.getRoleName()).isEqualTo("TESTER");
            verify(roleRepository).save(any(Role.class));
        }

        @Test
        @DisplayName("throws on duplicate role name")
        void throwsOnDuplicate() {
            when(roleRepository.findByRoleNameAndDeletedAtIsNull("TESTER")).thenReturn(Optional.of(sampleRole));
            assertThatThrownBy(() -> roleService.createRole(sampleRole))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("deleteRole")
    class DeleteRoleTests {

        @Test
        @DisplayName("soft-deletes custom role")
        void softDeletesCustomRole() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
            when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

            roleService.deleteRole(1L);

            verify(rolePermissionRepository).deleteByRoleId(1L);
            verify(userRoleRepository).deleteByRoleId(1L);
            verify(roleRepository).save(argThat(r -> r.getDeletedAt() != null));
        }

        @Test
        @DisplayName("blocks deletion of ADMIN system role")
        void blocksAdminDeletion() {
            Role adminRole = new Role();
            adminRole.setRoleId(2L);
            adminRole.setRoleName("ADMIN");
            when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));

            assertThatThrownBy(() -> roleService.deleteRole(2L))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("blocks deletion of MERCHANT system role")
        void blocksMerchantDeletion() {
            Role merchantRole = new Role();
            merchantRole.setRoleId(3L);
            merchantRole.setRoleName("MERCHANT");
            when(roleRepository.findById(3L)).thenReturn(Optional.of(merchantRole));

            assertThatThrownBy(() -> roleService.deleteRole(3L))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
