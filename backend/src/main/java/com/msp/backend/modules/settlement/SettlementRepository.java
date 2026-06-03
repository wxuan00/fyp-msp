package com.msp.backend.modules.settlement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long>,
        JpaSpecificationExecutor<Settlement> {

    List<Settlement> findAllByOrderBySettlementDateDesc();

    List<Settlement> findByCreditAdviceId(Long creditAdviceId);

    List<Settlement> findByCreditAdviceIdInOrderBySettlementDateDesc(List<Long> creditAdviceIds);

    List<Settlement> findBySettlementNoContainingIgnoreCase(String settlementNo);

    Page<Settlement> findByCreditAdviceIdIn(List<Long> creditAdviceIds, Pageable pageable);

    @Query("SELECT s FROM Settlement s LEFT JOIN FETCH s.creditAdvice ca LEFT JOIN FETCH ca.merchant WHERE s.settlementId = :id")
    Optional<Settlement> findByIdWithMerchant(@Param("id") Long id);

    @Query("SELECT s FROM Settlement s LEFT JOIN FETCH s.creditAdvice ca LEFT JOIN FETCH ca.merchant WHERE s.creditAdviceId = :creditAdviceId ORDER BY s.settlementDate DESC")
    List<Settlement> findByCreditAdviceIdWithMerchant(@Param("creditAdviceId") Long creditAdviceId);
}
