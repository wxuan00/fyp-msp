package com.msp.backend.modules.settlement;

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
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final CreditAdviceRepository creditAdviceRepository;

    public List<Settlement> getAllSettlements() {
        return settlementRepository.findAllByOrderBySettlementDateDesc();
    }

    public List<Settlement> getSettlementsByMerchantId(Long merchantId) {
        List<CreditAdvice> advices = creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(merchantId);
        List<Long> adviceIds = advices.stream().map(CreditAdvice::getCreditAdviceId).toList();
        if (adviceIds.isEmpty()) return List.of();
        return settlementRepository.findByCreditAdviceIdInOrderBySettlementDateDesc(adviceIds);
    }

    @Transactional(readOnly = true)
    public Settlement getSettlementById(Long id) {
        return settlementRepository.findByIdWithMerchant(id)
                .orElseThrow(() -> new RuntimeException("Settlement not found"));
    }

    @Transactional(readOnly = true)
    public java.util.List<Settlement> getSettlementsByCreditAdviceId(Long creditAdviceId) {
        return settlementRepository.findByCreditAdviceIdWithMerchant(creditAdviceId);
    }

    public Page<Settlement> getSettlementsPage(
            Long restrictToMerchantId,
            String merchantName,
            String settlementNo,
            String settlementType,
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

        // For merchant users, get their credit advice IDs first
        List<Long> allowedCreditAdviceIds = null;
        if (restrictToMerchantId != null) {
            List<CreditAdvice> advices = creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(restrictToMerchantId);
            allowedCreditAdviceIds = advices.stream().map(CreditAdvice::getCreditAdviceId).toList();
            if (allowedCreditAdviceIds.isEmpty()) {
                return Page.empty(pageable);
            }
        }

        final List<Long> finalAllowedCreditAdviceIds = allowedCreditAdviceIds;

        Specification<Settlement> spec = (root, query, cb) -> {
            boolean isCountQuery = query != null && Long.class.equals(query.getResultType());
            jakarta.persistence.criteria.Join<Object, Object> merchantJoin = null;

            if (!isCountQuery) {
                var caJoin = root.join("creditAdvice", jakarta.persistence.criteria.JoinType.LEFT);
                merchantJoin = caJoin.join("merchant", jakarta.persistence.criteria.JoinType.LEFT);
                if (sortByMerchant) {
                    query.orderBy(sortDir.equalsIgnoreCase("asc")
                            ? cb.asc(merchantJoin.get("merchantName"))
                            : cb.desc(merchantJoin.get("merchantName")));
                } else {
                    query.distinct(true);
                }
            }

            List<Predicate> predicates = new ArrayList<>();

            if (finalAllowedCreditAdviceIds != null) {
                predicates.add(root.get("creditAdviceId").in(finalAllowedCreditAdviceIds));
            }
            if (merchantName != null && !merchantName.isBlank()) {
                var mj = isCountQuery
                        ? root.join("creditAdvice", jakarta.persistence.criteria.JoinType.LEFT)
                              .join("merchant", jakarta.persistence.criteria.JoinType.LEFT)
                        : merchantJoin;
                predicates.add(cb.like(cb.lower(mj.get("merchantName")), "%" + merchantName.toLowerCase().trim() + "%"));
            }
            if (settlementNo != null && !settlementNo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("settlementNo")), "%" + settlementNo.toLowerCase().trim() + "%"));
            }
            if (settlementType != null && !settlementType.isBlank()) {
                predicates.add(cb.equal(root.get("settlementType"), settlementType));
            }
            if (dateFrom != null && !dateFrom.isBlank()) {
                LocalDateTime from = LocalDate.parse(dateFrom).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("settlementDate"), from));
            }
            if (dateTo != null && !dateTo.isBlank()) {
                LocalDateTime to = LocalDate.parse(dateTo).atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("settlementDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Settlement> result = settlementRepository.findAll(spec, pageable);
        return result;
    }

}
