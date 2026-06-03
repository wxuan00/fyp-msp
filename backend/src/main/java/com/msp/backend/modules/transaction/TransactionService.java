package com.msp.backend.modules.transaction;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> getAllTransactions() {
        List<Transaction> txns = transactionRepository.findAllByOrderByTxnDateDesc();
        return txns;
    }

    public List<Transaction> getTransactionsByMerchantId(Long merchantId) {
        List<Transaction> txns = transactionRepository.findByMerchantIdOrderByTxnDateDesc(merchantId);
        return txns;
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findByIdWithMerchant(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public Page<Transaction> getTransactionsPage(
            List<Long> restrictToMerchantIds,
            String merchantName,
            String txnId,
            String cardNo,
            String status,
            String channel,
            String dateFrom,
            String dateTo,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        // When sorting by merchantName, use query.orderBy inside the Specification
        // because Sort.by("merchant.merchantName") in Pageable conflicts with the JOIN
        boolean sortByMerchant = "merchantName".equals(sortBy);
        Sort sort = sortByMerchant
                ? Sort.unsorted()
                : (sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Skip JOIN on count query to avoid duplicate rows in pagination count
            boolean isCountQuery = query != null && Long.class.equals(query.getResultType());
            jakarta.persistence.criteria.Join<Object, Object> merchantJoin = null;

            if (!isCountQuery) {
                merchantJoin = root.join("merchant", jakarta.persistence.criteria.JoinType.LEFT);
                if (sortByMerchant) {
                    query.orderBy(sortDir.equalsIgnoreCase("asc")
                            ? cb.asc(merchantJoin.get("merchantName"))
                            : cb.desc(merchantJoin.get("merchantName")));
                } else {
                    query.distinct(true);
                }
            }

            if (restrictToMerchantIds != null && !restrictToMerchantIds.isEmpty()) {
                predicates.add(root.get("merchantId").in(restrictToMerchantIds));
            }
            if (merchantName != null && !merchantName.isBlank()) {
                // For count query, use a subquery-safe approach with a fresh join
                var mj = isCountQuery ? root.join("merchant", jakarta.persistence.criteria.JoinType.LEFT) : merchantJoin;
                predicates.add(cb.like(cb.lower(mj.get("merchantName")), "%" + merchantName.toLowerCase().trim() + "%"));
            }
            if (txnId != null && !txnId.isBlank()) {
                predicates.add(cb.like(cb.toString(root.get("transactionId")), "%" + txnId.trim() + "%"));
            }
            if (cardNo != null && !cardNo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("cardNo")), "%" + cardNo.toLowerCase().trim() + "%"));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (channel != null && !channel.isBlank()) {
                predicates.add(cb.equal(root.get("paymentChannel"), channel));
            }
            if (dateFrom != null && !dateFrom.isBlank()) {
                LocalDateTime from = LocalDate.parse(dateFrom).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("txnDate"), from));
            }
            if (dateTo != null && !dateTo.isBlank()) {
                LocalDateTime to = LocalDate.parse(dateTo).atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("txnDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable);
    }
}
