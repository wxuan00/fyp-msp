package com.msp.backend.modules.transaction;

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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService — Unit Tests")
class RefundServiceUnitTest {

    @Mock private RefundRepository refundRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private RefundService refundService;

    private Refund sampleRefund;
    private Transaction sampleTxn;

    @BeforeEach
    void setUp() {
        sampleTxn = new Transaction();
        sampleTxn.setTransactionId(100L);
        sampleTxn.setStatus("PENDING");
        sampleTxn.setMerchantId(10L);
        sampleTxn.setAmount(new BigDecimal("200.00"));
        sampleTxn.setCurrency("MYR");

        sampleRefund = new Refund();
        sampleRefund.setRefundId(1L);
        sampleRefund.setTransactionId(100L);
        sampleRefund.setMerchantId(10L);
        sampleRefund.setAmount(new BigDecimal("200.00"));
        sampleRefund.setRefundAmount(new BigDecimal("200.00"));
        sampleRefund.setCurrency("MYR");
        sampleRefund.setStatus("PENDING");
        sampleRefund.setRefundRefNo("RFD001");
    }

    @Nested @DisplayName("getAllRefunds")
    class GetAllTests {
        @Test @DisplayName("returns all refunds ordered desc")
        void returnsAll() {
            when(refundRepository.findAllByOrderBySubmissionDateDesc()).thenReturn(List.of(sampleRefund));
            List<Refund> result = refundService.getAllRefunds();
            assertThat(result).hasSize(1);
        }
    }

    @Nested @DisplayName("getRefundById")
    class GetByIdTests {
        @Test @DisplayName("returns refund when found")
        void returnsRefund() {
            when(refundRepository.findByIdWithMerchant(1L)).thenReturn(Optional.of(sampleRefund));
            Refund result = refundService.getRefundById(1L);
            assertThat(result.getRefundRefNo()).isEqualTo("RFD001");
        }

        @Test @DisplayName("throws when not found")
        void throwsWhenNotFound() {
            when(refundRepository.findByIdWithMerchant(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> refundService.getRefundById(99L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested @DisplayName("submitRefund")
    class SubmitRefundTests {
        @Test @DisplayName("sets status to PENDING and marks txn as REFUND_REQUESTED")
        void submitsRefund() {
            when(refundRepository.save(any(Refund.class))).thenReturn(sampleRefund);
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(sampleTxn));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTxn);
            Refund result = refundService.requestRefund(sampleRefund);
            assertThat(result.getStatus()).isEqualTo("PENDING");
            verify(transactionRepository).save(argThat(t -> "REFUND_REQUESTED".equals(t.getStatus())));
        }
    }

    @Nested @DisplayName("cancelRefund")
    class CancelRefundTests {
        @Test @DisplayName("cancels pending refund and resets txn status")
        void cancelsPending() {
            when(refundRepository.findById(1L)).thenReturn(Optional.of(sampleRefund));
            when(refundRepository.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.findById(100L)).thenReturn(Optional.of(sampleTxn));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTxn);
            Refund result = refundService.cancelRefund(1L);
            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test @DisplayName("throws when refund is not PENDING")
        void throwsWhenNotPending() {
            sampleRefund.setStatus("APPROVED");
            when(refundRepository.findById(1L)).thenReturn(Optional.of(sampleRefund));
            assertThatThrownBy(() -> refundService.cancelRefund(1L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested @DisplayName("searchRefunds (paginated)")
    class SearchTests {
        @Test @DisplayName("returns paged results")
        void returnsPaged() {
            Page<Refund> page = new PageImpl<>(List.of(sampleRefund));
            when(refundRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(page);
            Page<Refund> result = refundService.getRefundsPage(
                    null, null, null, null, null, null, null, null, null, 0, 10, "submissionDate", "desc");
            assertThat(result.getContent()).hasSize(1);
        }
    }
}
