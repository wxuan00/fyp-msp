package com.msp.backend.modules.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByMerchantIdOrderByTxnDateDesc(Long merchantId);

    List<Transaction> findAllByOrderByTxnDateDesc();

    List<Transaction> findBySettlementId(Long settlementId);

    Page<Transaction> findByMerchantId(Long merchantId, Pageable pageable);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.merchant WHERE t.transactionId = :id")
    Optional<Transaction> findByIdWithMerchant(@Param("id") Long id);
}
