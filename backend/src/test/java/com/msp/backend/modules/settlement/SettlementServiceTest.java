package com.msp.backend.modules.settlement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementService Unit Tests")
class SettlementServiceTest {

    @Mock private SettlementRepository settlementRepository;
    @Mock private CreditAdviceRepository creditAdviceRepository;

    @InjectMocks
    private SettlementService settlementService;

    private Settlement sampleSettlement;
    private CreditAdvice sampleAdvice;

    @BeforeEach
    void setUp() {
        sampleAdvice = new CreditAdvice();
        sampleAdvice.setCreditAdviceId(1L);
        sampleAdvice.setMerchantId(10L);
        sampleAdvice.setAccountNo("ACC001");
        sampleAdvice.setPaymentDate(LocalDateTime.now());

        sampleSettlement = new Settlement();
        sampleSettlement.setSettlementId(1L);
        sampleSettlement.setSettlementNo("SET001");
        sampleSettlement.setCreditAdviceId(1L);
        sampleSettlement.setSettlementType("FULL");
        sampleSettlement.setSettlementAmount(new BigDecimal("5000.00"));
        sampleSettlement.setSettlementDate(LocalDateTime.now());
    }

    // ─── getAllSettlements ────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllSettlements: returns all ordered by date desc")
    void getAllSettlements_returnsAll() {
        when(settlementRepository.findAllByOrderBySettlementDateDesc())
                .thenReturn(List.of(sampleSettlement));
        List<Settlement> result = settlementService.getAllSettlements();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSettlementNo()).isEqualTo("SET001");
    }

    @Test
    @DisplayName("getAllSettlements: returns empty when none exist")
    void getAllSettlements_empty() {
        when(settlementRepository.findAllByOrderBySettlementDateDesc()).thenReturn(List.of());
        assertThat(settlementService.getAllSettlements()).isEmpty();
    }

    // ─── getSettlementsByMerchantId ───────────────────────────────────────────

    @Test
    @DisplayName("getSettlementsByMerchantId: returns settlements for merchant")
    void getSettlementsByMerchantId_returnsList() {
        when(creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(10L))
                .thenReturn(List.of(sampleAdvice));
        when(settlementRepository.findByCreditAdviceIdInOrderBySettlementDateDesc(List.of(1L)))
                .thenReturn(List.of(sampleSettlement));

        List<Settlement> result = settlementService.getSettlementsByMerchantId(10L);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getSettlementsByMerchantId: returns empty when merchant has no credit advices")
    void getSettlementsByMerchantId_noAdvices_empty() {
        when(creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(999L))
                .thenReturn(List.of());
        assertThat(settlementService.getSettlementsByMerchantId(999L)).isEmpty();
    }

    // ─── getSettlementById ────────────────────────────────────────────────────

    @Test
    @DisplayName("getSettlementById: returns settlement when found")
    void getSettlementById_found() {
        when(settlementRepository.findByIdWithMerchant(1L)).thenReturn(Optional.of(sampleSettlement));
        Settlement result = settlementService.getSettlementById(1L);
        assertThat(result.getSettlementNo()).isEqualTo("SET001");
    }

    @Test
    @DisplayName("getSettlementById: throws when not found")
    void getSettlementById_notFound_throws() {
        when(settlementRepository.findByIdWithMerchant(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> settlementService.getSettlementById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Settlement not found");
    }

    // ─── getSettlementsByCreditAdviceId ──────────────────────────────────────

    @Test
    @DisplayName("getSettlementsByCreditAdviceId: returns settlements for advice")
    void getSettlementsByCreditAdviceId_returnsList() {
        when(settlementRepository.findByCreditAdviceIdWithMerchant(1L))
                .thenReturn(List.of(sampleSettlement));
        List<Settlement> result = settlementService.getSettlementsByCreditAdviceId(1L);
        assertThat(result).hasSize(1);
    }

    // ─── getSettlementsPage ───────────────────────────────────────────────────

    @Test
    @DisplayName("getSettlementsPage: returns paged results without restriction")
    void getSettlementsPage_noRestriction_returnsPage() {
        Page<Settlement> mockPage = new PageImpl<>(List.of(sampleSettlement));
        when(settlementRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<Settlement> result = settlementService.getSettlementsPage(
                null, null, null, null, null, null, 0, 10, "settlementDate", "desc");

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getSettlementsPage: returns empty page when merchant has no advices")
    void getSettlementsPage_merchantNoAdvices_emptyPage() {
        when(creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(999L))
                .thenReturn(List.of());

        Page<Settlement> result = settlementService.getSettlementsPage(
                999L, null, null, null, null, null, 0, 10, "settlementDate", "desc");

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("getSettlementsPage: filters by settlement type")
    void getSettlementsPage_filterByType() {
        Page<Settlement> mockPage = new PageImpl<>(List.of(sampleSettlement));
        when(settlementRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<Settlement> result = settlementService.getSettlementsPage(
                null, null, null, "FULL", null, null, 0, 10, "settlementDate", "desc");

        assertThat(result.getContent()).hasSize(1);
    }
}
