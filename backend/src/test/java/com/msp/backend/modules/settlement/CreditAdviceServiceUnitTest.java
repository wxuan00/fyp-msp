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
 * Unit Tests for CreditAdviceService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreditAdviceService — Unit Tests")
class CreditAdviceServiceUnitTest {

    @Mock
    private CreditAdviceRepository creditAdviceRepository;

    @InjectMocks
    private CreditAdviceService creditAdviceService;

    private CreditAdvice sampleCA;

    @BeforeEach
    void setUp() {
        sampleCA = new CreditAdvice();
        sampleCA.setCreditAdviceId(1L);
        sampleCA.setMerchantId(10L);
        sampleCA.setAccountNo("ACC001");
        sampleCA.setAmount(new BigDecimal("2500.00"));
        sampleCA.setCurrency("MYR");
        sampleCA.setPaymentDate(LocalDateTime.now());
    }

    @Nested
    @DisplayName("getAllCreditAdvices")
    class GetAllTests {

        @Test
        @DisplayName("returns all credit advices ordered by paymentDate desc")
        void returnsAll() {
            when(creditAdviceRepository.findAllByOrderByPaymentDateDesc()).thenReturn(List.of(sampleCA));
            List<CreditAdvice> result = creditAdviceService.getAllCreditAdvices();
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getCreditAdvicesByMerchantId")
    class GetByMerchantTests {

        @Test
        @DisplayName("returns credit advices for merchant")
        void returnsByMerchant() {
            when(creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(10L))
                    .thenReturn(List.of(sampleCA));
            List<CreditAdvice> result = creditAdviceService.getCreditAdvicesByMerchantId(10L);
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getCreditAdviceById")
    class GetByIdTests {

        @Test
        @DisplayName("returns credit advice when found")
        void returnsCA() {
            when(creditAdviceRepository.findByIdWithMerchant(1L)).thenReturn(Optional.of(sampleCA));
            CreditAdvice result = creditAdviceService.getCreditAdviceById(1L);
            assertThat(result.getAccountNo()).isEqualTo("ACC001");
        }

        @Test
        @DisplayName("throws when not found")
        void throwsWhenNotFound() {
            when(creditAdviceRepository.findByIdWithMerchant(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> creditAdviceService.getCreditAdviceById(99L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("searchCreditAdvices (paginated)")
    class SearchTests {

        @Test
        @DisplayName("returns paged results")
        void returnsPagedResults() {
            Page<CreditAdvice> page = new PageImpl<>(List.of(sampleCA));
            when(creditAdviceRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(page);

            Page<CreditAdvice> result = creditAdviceService.getCreditAdvicesPage(
                    null, null, null, null, null, 0, 10, "paymentDate", "desc");
            assertThat(result.getContent()).hasSize(1);
        }
    }
}
