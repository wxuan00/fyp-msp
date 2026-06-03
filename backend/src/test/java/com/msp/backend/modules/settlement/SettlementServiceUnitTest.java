package com.msp.backend.modules.settlement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for SettlementService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementService — Unit Tests")
class SettlementServiceUnitTest {

    @Mock private SettlementRepository settlementRepository;
    @Mock private CreditAdviceRepository creditAdviceRepository;

    @InjectMocks
    private SettlementService settlementService;

    private Settlement sampleSettlement;
    private CreditAdvice sampleCA;

    @BeforeEach
    void setUp() {
        sampleCA = new CreditAdvice();
        sampleCA.setCreditAdviceId(1L);
        sampleCA.setMerchantId(10L);

        sampleSettlement = new Settlement();
        sampleSettlement.setSettlementId(1L);
        sampleSettlement.setCreditAdviceId(1L);
        sampleSettlement.setSettlementNo("STL001");
        sampleSettlement.setSettlementAmount(new BigDecimal("5000.00"));
        sampleSettlement.setCurrency("MYR");
        sampleSettlement.setSettlementDate(LocalDateTime.now());
    }

    @Nested
    @DisplayName("getAllSettlements")
    class GetAllTests {

        @Test
        @DisplayName("returns all settlements ordered by date desc")
        void returnsAll() {
            when(settlementRepository.findAllByOrderBySettlementDateDesc()).thenReturn(List.of(sampleSettlement));
            List<Settlement> result = settlementService.getAllSettlements();
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getSettlementsByMerchantId")
    class GetByMerchantTests {

        @Test
        @DisplayName("returns settlements linked to merchant via credit advice")
        void returnsByMerchant() {
            when(creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(10L))
                    .thenReturn(List.of(sampleCA));
            when(settlementRepository.findByCreditAdviceIdInOrderBySettlementDateDesc(List.of(1L)))
                    .thenReturn(List.of(sampleSettlement));

            List<Settlement> result = settlementService.getSettlementsByMerchantId(10L);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("returns empty when no credit advices for merchant")
        void returnsEmptyWhenNoCA() {
            when(creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(99L))
                    .thenReturn(List.of());

            List<Settlement> result = settlementService.getSettlementsByMerchantId(99L);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSettlementById")
    class GetByIdTests {

        @Test
        @DisplayName("returns settlement when found")
        void returnsSettlement() {
            when(settlementRepository.findByIdWithMerchant(1L)).thenReturn(Optional.of(sampleSettlement));
            Settlement result = settlementService.getSettlementById(1L);
            assertThat(result.getSettlementNo()).isEqualTo("STL001");
        }

        @Test
        @DisplayName("throws when not found")
        void throwsWhenNotFound() {
            when(settlementRepository.findByIdWithMerchant(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> settlementService.getSettlementById(99L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("searchSettlements (paginated)")
    class SearchTests {

        @Test
        @DisplayName("returns paged results")
        void returnsPagedResults() {
            Page<Settlement> page = new PageImpl<>(List.of(sampleSettlement));
            when(settlementRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(page);

            Page<Settlement> result = settlementService.getSettlementsPage(
                    null, null, null, null, null, null, 0, 10, "settlementDate", "desc");
            assertThat(result.getContent()).hasSize(1);
        }
    }
}
