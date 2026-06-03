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
public class CreditAdviceService {

    private final CreditAdviceRepository creditAdviceRepository;

    public List<CreditAdvice> getAllCreditAdvices() {
        return creditAdviceRepository.findAllByOrderByPaymentDateDesc();
    }

    public List<CreditAdvice> getCreditAdvicesByMerchantId(Long merchantId) {
        return creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(merchantId);
    }

    @Transactional(readOnly = true)
    public CreditAdvice getCreditAdviceById(Long id) {
        return creditAdviceRepository.findByIdWithMerchant(id)
                .orElseThrow(() -> new RuntimeException("Credit advice not found"));
    }

    public Page<CreditAdvice> getCreditAdvicesPage(
            Long restrictToMerchantId,
            String merchantName,
            String accountId,
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

        Specification<CreditAdvice> spec = (root, query, cb) -> {
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

            if (restrictToMerchantId != null) {
                predicates.add(cb.equal(root.get("merchantId"), restrictToMerchantId));
            }
            if (merchantName != null && !merchantName.isBlank()) {
                var mj = isCountQuery ? root.join("merchant", jakarta.persistence.criteria.JoinType.LEFT) : merchantJoin;
                predicates.add(cb.like(cb.lower(mj.get("merchantName")), "%" + merchantName.toLowerCase().trim() + "%"));
            }
            if (accountId != null && !accountId.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("accountId")), "%" + accountId.toLowerCase().trim() + "%"));
            }
            if (dateFrom != null && !dateFrom.isBlank()) {
                LocalDateTime from = LocalDate.parse(dateFrom).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), from));
            }
            if (dateTo != null && !dateTo.isBlank()) {
                LocalDateTime to = LocalDate.parse(dateTo).atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("paymentDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return creditAdviceRepository.findAll(spec, pageable);
    }
}
