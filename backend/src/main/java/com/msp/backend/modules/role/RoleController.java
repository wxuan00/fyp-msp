package com.msp.backend.modules.role;

import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.util.AuditHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final UserRepository userRepository;

    @GetMapping
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @GetMapping("/{id}")
    public Role getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    @GetMapping("/permissions/all")
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @GetMapping("/{id}/permissions")
    public List<Permission> getRolePermissions(@PathVariable Long id) {
        List<RolePermission> rps = rolePermissionRepository.findByRoleId(id);
        List<Long> permIds = rps.stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
        if (permIds.isEmpty()) return List.of();
        return permissionRepository.findAllById(permIds);
    }

    @Transactional
    @PutMapping("/{id}/permissions")
    public ResponseEntity<Void> updateRolePermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        String actor = AuditHelper.currentUser();
        rolePermissionRepository.deleteByRoleId(id);
        rolePermissionRepository.flush();
        entityManager.clear();
        for (Long permId : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(id);
            rp.setPermissionId(permId);
            rp.setCreatedBy(actor);
            rp.setLastModifiedBy(actor);
            rp.setLastModifiedAt(java.time.LocalDateTime.now());
            rolePermissionRepository.save(rp);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/with-permissions")
    public List<Map<String, Object>> getAllRolesWithPermissions() {
        List<Role> roles = roleService.getAllRoles();
        List<Permission> allPerms = permissionRepository.findAll();
        List<RolePermission> allRPs = rolePermissionRepository.findAll();

        return roles.stream().map(role -> {
            List<Long> permIds = allRPs.stream()
                .filter(rp -> rp.getRoleId().equals(role.getRoleId()))
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
            List<Permission> rolePerms = allPerms.stream()
                .filter(p -> permIds.contains(p.getPermissionId()))
                .collect(Collectors.toList());
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("roleId", role.getRoleId());
            map.put("roleName", role.getRoleName());
            map.put("roleType", role.getRoleType() != null ? role.getRoleType() : "");
            map.put("description", role.getDescription() != null ? role.getDescription() : "");
            map.put("createdAt", role.getCreatedAt() != null ? role.getCreatedAt().toString() : "");
            map.put("createdBy", AuditHelper.resolveDisplayName(role.getCreatedBy(), userRepository));
            map.put("lastModifiedAt", role.getLastModifiedAt() != null ? role.getLastModifiedAt().toString() : "");
            map.put("lastModifiedBy", AuditHelper.resolveDisplayName(role.getLastModifiedBy(), userRepository));
            map.put("permissions", rolePerms);
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        return ResponseEntity.ok(roleService.createRole(role));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        return ResponseEntity.ok(roleService.updateRole(id, role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
