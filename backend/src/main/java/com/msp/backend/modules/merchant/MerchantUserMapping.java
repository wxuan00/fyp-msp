package com.msp.backend.modules.merchant;

import com.msp.backend.modules.user.User;
import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "merchant_users")
@IdClass(MerchantUserMapping.MerchantUserId.class)
public class MerchantUserMapping {

    @Id
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // JPA relationship for ERD FK: merchant_users.merchant_id → merchants
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_mu_merchant"))
    private Merchant merchant;

    // JPA relationship for ERD FK: merchant_users.user_id → users
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_mu_user"))
    private User user;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.lastModifiedAt == null) this.lastModifiedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedAt = LocalDateTime.now();
    }

    @Data
    public static class MerchantUserId implements Serializable {
        private Long merchantId;
        private Long userId;
    }
}
