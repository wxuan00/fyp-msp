package com.msp.backend.modules.merchant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    List<Merchant> findByMerchantNameContainingIgnoreCase(String name);
}