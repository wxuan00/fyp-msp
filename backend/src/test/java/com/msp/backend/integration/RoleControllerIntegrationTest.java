package com.msp.backend.integration;

import com.msp.backend.modules.role.*;
import com.msp.backend.modules.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleController — Integration Tests")
class RoleControllerIntegrationTest {

    @Mock private RoleService roleService;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private jakarta.persistence.EntityManager entityManager;
    @Mock private UserRepository userRepository;

    @InjectMocks private RoleController roleController;

    @Nested @DisplayName("GET /api/roles")
    class GetAllTests {

        @Test @DisplayName("returns non-deleted roles")
        void returnsRoles() {
            Role r = new Role(); r.setRoleId(1L); r.setRoleName("ADMIN");
            when(roleService.getAllRoles()).thenReturn(List.of(r));
            List<Role> result = roleController.getAllRoles();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRoleName()).isEqualTo("ADMIN");
        }
    }

    @Nested @DisplayName("GET /api/roles/{id}")
    class GetByIdTests {

        @Test @DisplayName("returns role when found")
        void returnsRole() {
            Role r = new Role(); r.setRoleId(1L); r.setRoleName("ADMIN");
            when(roleService.getRoleById(1L)).thenReturn(r);
            Role result = roleController.getRoleById(1L);
            assertThat(result.getRoleName()).isEqualTo("ADMIN");
        }
    }

    @Nested @DisplayName("GET /api/roles/permissions/all")
    class GetAllPermissionsTests {

        @Test @DisplayName("returns all permissions")
        void returnsPermissions() {
            Permission p = new Permission(); p.setPermissionId(1L); p.setPermissionName("VIEW_USERS");
            when(permissionRepository.findAll()).thenReturn(List.of(p));
            List<Permission> result = roleController.getAllPermissions();
            assertThat(result).hasSize(1);
        }
    }

    @Nested @DisplayName("DELETE /api/roles/{id}")
    class DeleteTests {

        @Test @DisplayName("blocks deletion of system role ADMIN")
        void blocksAdminDeletion() {
            doThrow(new RuntimeException("Cannot delete system role")).when(roleService).deleteRole(1L);
            assertThatThrownBy(() -> roleController.deleteRole(1L))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test @DisplayName("deletes custom role successfully")
        void deletesCustomRole() {
            doNothing().when(roleService).deleteRole(5L);
            ResponseEntity<Void> resp = roleController.deleteRole(5L);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }
}
