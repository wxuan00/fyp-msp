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
public class RefundService {

    private final RefundRepository refundRepository;
    private final TransactionRepository transactionRepository;

    public List<Refund> getAllRefunds() {
        return refundRepository.findAllByOrderBySubmissionDateDesc();
    }

    public List<Refund> getRefundsByMerchantId(Long merchantId) {
        return refundRepository.findByMerchantIdOrderBySubmissionDateDesc(merchantId);
    }

    @Transactional(readOnly = true)
    public Refund getRefundById(Long id) {
        return refundRepository.findByIdWithMerchant(id)
                .orElseThrow(() -> new RuntimeException("Refund not found"));
    }

    @Transactional
    public Refund requestRefund(Refund refund) {
        refund.setStatus("PENDING");
        Refund saved = refundRepository.save(refund);
        // Mark the original transaction as refund-requested
        if (refund.getTransactionId() != null) {
            transactionRepository.findById(refund.getTransactionId()).ifPresent(txn -> {
                txn.setStatus("REFUND_REQUESTED");
                transactionRepository.save(txn);
            });
        }
        return saved;
    }

    @Transactional
    public Refund cancelRefund(Long id) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Refund not found"));
        if (!"PENDING".equals(refund.getStatus())) {
            throw new RuntimeException("Only PENDING refunds can be cancelled");
        }
        refund.setStatus("CANCELLED");
        Refund saved = refundRepository.save(refund);
        // Mark the original transaction as refund-cancelled so history is preserved
        if (refund.getTransactionId() != null) {
            transactionRepository.findById(refund.getTransactionId()).ifPresent(txn -> {
                txn.setStatus("PENDING");
                transactionRepository.save(txn);
            });
        }
        return saved;
    }

    public Page<Refund> getRefundsPage(
            List<Long> restrictToMerchantIds,
            String merchantName,
            String refundRefNo,
            String transactionId,
            String cardNo,
            String status,
            String refundType,
            String dateFrom,
            String dateTo,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        boolean sortByMerchant = "merchantName".equals(sortBy);
        Sort sort = sortByMerchant
                ? Sort.unsorted()
                : (sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Refund> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

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
                var mj = isCountQuery ? root.join("merchant", jakarta.persistence.criteria.JoinType.LEFT) : merchantJoin;
                predicates.add(cb.like(cb.lower(mj.get("merchantName")), "%" + merchantName.toLowerCase().trim() + "%"));
            }
            if (refundRefNo != null && !refundRefNo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("refundRefNo")), "%" + refundRefNo.toLowerCase().trim() + "%"));
            }
            if (transactionId != null && !transactionId.isBlank()) {
                predicates.add(cb.like(cb.toString(root.get("transactionId")), "%" + transactionId.trim() + "%"));
            }
            if (cardNo != null && !cardNo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("cardNo")), "%" + cardNo.toLowerCase().trim() + "%"));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (refundType != null && !refundType.isBlank()) {
                predicates.add(cb.equal(root.get("refundType"), refundType));
            }
            if (dateFrom != null && !dateFrom.isBlank()) {
                LocalDateTime from = LocalDate.parse(dateFrom).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("submissionDate"), from));
            }
            if (dateTo != null && !dateTo.isBlank()) {
                LocalDateTime to = LocalDate.parse(dateTo).atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("submissionDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return refundRepository.findAll(spec, pageable);
    }
}
