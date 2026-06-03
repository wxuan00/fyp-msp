package com.msp.backend.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByUserId(Long userId);
    void deleteByUserId(Long userId);
    void deleteByRoleId(Long roleId);
}
