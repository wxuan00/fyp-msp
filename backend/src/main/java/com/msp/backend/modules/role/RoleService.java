package com.msp.backend.modules.role;

import com.msp.backend.modules.user.UserRoleRepository;
import com.msp.backend.util.AuditHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;

    public List<Role> getAllRoles() {
        return roleRepository.findByDeletedAtIsNull();
    }

    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    public Role createRole(Role role) {
        if (roleRepository.findByRoleNameAndDeletedAtIsNull(role.getRoleName()).isPresent()) {
            throw new RuntimeException("Role name already exists");
        }
        role.setCreatedBy(AuditHelper.currentUser());
        role.setLastModifiedBy(AuditHelper.currentUser());
        return roleRepository.save(role);
    }

    public Role updateRole(Long id, Role updated) {
        Role role = getRoleById(id);
        role.setRoleName(updated.getRoleName());
        role.setDescription(updated.getDescription());
        role.setRoleType(updated.getRoleType());
        role.setLastModifiedBy(AuditHelper.currentUser());
        role.setLastModifiedAt(java.time.LocalDateTime.now());
        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = getRoleById(id);
        if ("ADMIN".equals(role.getRoleName()) || "MERCHANT".equals(role.getRoleName())) {
            throw new RuntimeException("Cannot delete system role: " + role.getRoleName());
        }
        // Remove FK-referencing rows
        rolePermissionRepository.deleteByRoleId(id);
        userRoleRepository.deleteByRoleId(id);
        // Soft delete
        role.setDeletedAt(java.time.LocalDateTime.now());
        role.setLastModifiedBy(AuditHelper.currentUser());
        role.setLastModifiedAt(java.time.LocalDateTime.now());
        roleRepository.save(role);
    }
}
