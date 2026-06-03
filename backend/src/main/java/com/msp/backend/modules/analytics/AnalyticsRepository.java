package com.msp.backend.modules.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {

    List<Analytics> findByMerchantIdOrderByGeneratedAtDesc(Long merchantId);

    List<Analytics> findByDataNameOrderByGeneratedAtDesc(String dataName);
}
