package com.msp.backend.util;

import com.msp.backend.modules.merchant.MerchantUserMappingRepository;
import com.msp.backend.modules.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves the merchant ID(s) for a logged-in user.
 *
 * Supports the many-to-many merchant_users mapping table: one user can be
 * linked to multiple merchants and one merchant can have multiple users.
 */
@Component
@RequiredArgsConstructor
public class MerchantResolver {

    private final MerchantUserMappingRepository merchantUserMappingRepository;

    /**
     * Returns the primary (first) merchant ID for the user, or null if not linked.
     * Kept for backward compat where a single ID is needed.
     */
    public Long resolveForUser(User user) {
        var mappings = merchantUserMappingRepository.findByUserId(user.getUserId());
        return mappings.isEmpty() ? null : mappings.get(0).getMerchantId();
    }

    /**
     * Returns ALL merchant IDs for the user (many-to-many support).
     * Returns an empty list if the user has no merchant mappings.
     */
    public List<Long> resolveAllForUser(User user) {
        return merchantUserMappingRepository.findByUserId(user.getUserId())
                .stream()
                .map(m -> m.getMerchantId())
                .collect(Collectors.toList());
    }
}
