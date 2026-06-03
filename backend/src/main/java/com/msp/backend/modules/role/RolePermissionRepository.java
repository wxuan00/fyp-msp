package com.msp.backend.modules.role;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    List<RolePermission> findByRoleId(Long roleId);
    void deleteByRoleId(Long roleId);
}
