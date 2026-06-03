package com.msp.backend.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    Optional<User> findByDisplayNameIgnoreCase(String displayName);
    Optional<User> findByDisplayNameIgnoreCaseAndDeletedAtIsNull(String displayName);
    Optional<User> findByResetToken(String resetToken);
    List<User> findByDeletedAtIsNull();
    List<User> findByDeletedAtIsNullAndStatus(String status);
    List<User> findByLastLoginAtBeforeAndDeletedAtIsNullAndStatus(java.time.LocalDateTime cutoff, String status);
    boolean existsByEmail(String email);
    boolean existsByDisplayNameIgnoreCaseAndUserIdNot(String displayName, Long userId);
    boolean existsByDisplayNameIgnoreCase(String displayName);

    @org.springframework.data.jpa.repository.Query("SELECT CAST(u.userId AS string) FROM User u WHERE CAST(u.userId AS string) LIKE :prefix%")
    List<String> findUserIdsByPrefix(@org.springframework.data.repository.query.Param("prefix") String prefix);

    // Soft-delete-aware uniqueness checks (only considers non-deleted records)
    boolean existsByEmailAndDeletedAtIsNull(String email);
    boolean existsByDisplayNameIgnoreCaseAndDeletedAtIsNull(String displayName);
    boolean existsByDisplayNameIgnoreCaseAndUserIdNotAndDeletedAtIsNull(String displayName, Long userId);
}