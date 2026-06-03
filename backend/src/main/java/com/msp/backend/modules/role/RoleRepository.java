package com.msp.backend.modules.role;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(String roleName);
    Optional<Role> findByRoleNameAndDeletedAtIsNull(String roleName);
    java.util.List<Role> findByDeletedAtIsNull();
}
