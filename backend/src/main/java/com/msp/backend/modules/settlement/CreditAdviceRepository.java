package com.msp.backend.modules.settlement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CreditAdviceRepository extends JpaRepository<CreditAdvice, Long>,
        JpaSpecificationExecutor<CreditAdvice> {

    List<CreditAdvice> findByMerchantIdOrderByPaymentDateDesc(Long merchantId);

    List<CreditAdvice> findAllByOrderByPaymentDateDesc();

    Page<CreditAdvice> findByMerchantId(Long merchantId, Pageable pageable);

    @Query("SELECT ca FROM CreditAdvice ca LEFT JOIN FETCH ca.merchant WHERE ca.creditAdviceId = :id")
    Optional<CreditAdvice> findByIdWithMerchant(@Param("id") Long id);
}
