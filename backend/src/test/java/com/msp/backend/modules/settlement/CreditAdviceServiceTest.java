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
@DisplayName("CreditAdviceService Unit Tests")
class CreditAdviceServiceTest {

    @Mock private CreditAdviceRepository creditAdviceRepository;

    @InjectMocks
    private CreditAdviceService creditAdviceService;

    private CreditAdvice sampleAdvice;

    @BeforeEach
    void setUp() {
        sampleAdvice = new CreditAdvice();
        sampleAdvice.setCreditAdviceId(1L);
        sampleAdvice.setAccountNo("ACC001");
        sampleAdvice.setMerchantId(10L);
        sampleAdvice.setAmount(new BigDecimal("5000.00"));
        sampleAdvice.setPaymentDate(LocalDateTime.now());
        sampleAdvice.setCurrency("MYR");
    }

    // ─── getAllCreditAdvices ───────────────────────────────────────────────────

    @Test
    @DisplayName("getAllCreditAdvices: returns all ordered by date desc")
    void getAllCreditAdvices_returnsAll() {
        when(creditAdviceRepository.findAllByOrderByPaymentDateDesc())
                .thenReturn(List.of(sampleAdvice));
        List<CreditAdvice> result = creditAdviceService.getAllCreditAdvices();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountNo()).isEqualTo("ACC001");
    }

    @Test
    @DisplayName("getAllCreditAdvices: returns empty list when none")
    void getAllCreditAdvices_empty() {
        when(creditAdviceRepository.findAllByOrderByPaymentDateDesc()).thenReturn(List.of());
        assertThat(creditAdviceService.getAllCreditAdvices()).isEmpty();
    }

    // ─── getCreditAdvicesByMerchantId ─────────────────────────────────────────

    @Test
    @DisplayName("getCreditAdvicesByMerchantId: returns records for merchant")
    void getCreditAdvicesByMerchantId_returnsForMerchant() {
        when(creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(10L))
                .thenReturn(List.of(sampleAdvice));
        List<CreditAdvice> result = creditAdviceService.getCreditAdvicesByMerchantId(10L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMerchantId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getCreditAdvicesByMerchantId: returns empty for unknown merchant")
    void getCreditAdvicesByMerchantId_unknown_empty() {
        when(creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(999L)).thenReturn(List.of());
        assertThat(creditAdviceService.getCreditAdvicesByMerchantId(999L)).isEmpty();
    }

    // ─── getCreditAdviceById ──────────────────────────────────────────────────

    @Test
    @DisplayName("getCreditAdviceById: returns advice when found")
    void getCreditAdviceById_found() {
        when(creditAdviceRepository.findByIdWithMerchant(1L)).thenReturn(Optional.of(sampleAdvice));
        CreditAdvice result = creditAdviceService.getCreditAdviceById(1L);
        assertThat(result.getCreditAdviceId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCreditAdviceById: throws when not found")
    void getCreditAdviceById_notFound_throws() {
        when(creditAdviceRepository.findByIdWithMerchant(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> creditAdviceService.getCreditAdviceById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credit advice not found");
    }

    // ─── getCreditAdvicesPage ─────────────────────────────────────────────────

    @Test
    @DisplayName("getCreditAdvicesPage: returns paged results")
    void getCreditAdvicesPage_returnsPage() {
        Page<CreditAdvice> mockPage = new PageImpl<>(List.of(sampleAdvice));
        when(creditAdviceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<CreditAdvice> result = creditAdviceService.getCreditAdvicesPage(
                null, null, null, null, null, 0, 10, "paymentDate", "desc");

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getCreditAdvicesPage: filters by merchantId")
    void getCreditAdvicesPage_filterByMerchant() {
        Page<CreditAdvice> mockPage = new PageImpl<>(List.of(sampleAdvice));
        when(creditAdviceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<CreditAdvice> result = creditAdviceService.getCreditAdvicesPage(
                10L, null, null, null, null, 0, 10, "paymentDate", "asc");

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getCreditAdvicesPage: empty page when no match")
    void getCreditAdvicesPage_noMatch_empty() {
        Page<CreditAdvice> emptyPage = Page.empty();
        when(creditAdviceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<CreditAdvice> result = creditAdviceService.getCreditAdvicesPage(
                null, "NonExistent", null, null, null, 0, 10, "paymentDate", "desc");

        assertThat(result.getTotalElements()).isZero();
    }
}
