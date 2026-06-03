package com.msp.backend.modules.transaction;

import com.msp.backend.modules.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Helper to resolve merchant names from merchantId.
 * Used since we no longer denormalize merchantName on entities.
 */
@Component
@RequiredArgsConstructor
public class MerchantNameResolver {

    private final MerchantRepository merchantRepository;

    public String resolve(Long merchantId) {
        if (merchantId == null) return "Unknown";
        return merchantRepository.findById(merchantId)
                .map(m -> m.getMerchantName())
                .orElse("Unknown");
    }
}
