package com.msp.backend.modules.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long>,
        JpaSpecificationExecutor<Refund> {

    List<Refund> findByMerchantIdOrderBySubmissionDateDesc(Long merchantId);

    List<Refund> findAllByOrderBySubmissionDateDesc();

    List<Refund> findByTransactionId(Long transactionId);

    Page<Refund> findByMerchantId(Long merchantId, Pageable pageable);

    @Query("SELECT r FROM Refund r LEFT JOIN FETCH r.merchant WHERE r.refundId = :id")
    Optional<Refund> findByIdWithMerchant(@Param("id") Long id);
}
