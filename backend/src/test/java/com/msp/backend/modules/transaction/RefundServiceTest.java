package com.msp.backend.modules.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService Unit Tests")
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RefundService refundService;

    private Refund sampleRefund;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        sampleRefund = new Refund();
        sampleRefund.setRefundId(1L);
        sampleRefund.setTransactionId(10L);
        sampleRefund.setMerchantId(100L);
        sampleRefund.setStatus("PENDING");
        sampleRefund.setRefundAmount(new BigDecimal("50.00"));
        sampleRefund.setCurrency("MYR");
        sampleRefund.setSubmissionDate(LocalDateTime.now());

        sampleTransaction = new Transaction();
        sampleTransaction.setTransactionId(10L);
        sampleTransaction.setStatus("APPROVED");
        sampleTransaction.setAmount(new BigDecimal("100.00"));
        sampleTransaction.setCurrency("MYR");
    }

    // ─── getAllRefunds ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllRefunds: returns list from repository")
    void getAllRefunds_returnsList() {
        List<Refund> expected = Arrays.asList(sampleRefund);
        when(refundRepository.findAllByOrderBySubmissionDateDesc()).thenReturn(expected);

        List<Refund> result = refundService.getAllRefunds();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRefundId()).isEqualTo(1L);
        verify(refundRepository).findAllByOrderBySubmissionDateDesc();
    }

    @Test
    @DisplayName("getAllRefunds: returns empty list when no refunds exist")
    void getAllRefunds_emptyList() {
        when(refundRepository.findAllByOrderBySubmissionDateDesc()).thenReturn(List.of());

        List<Refund> result = refundService.getAllRefunds();

        assertThat(result).isEmpty();
    }

    // ─── getRefundsByMerchantId ──────────────────────────────────────────────────

    @Test
    @DisplayName("getRefundsByMerchantId: returns refunds for a given merchant")
    void getRefundsByMerchantId_returnsList() {
        when(refundRepository.findByMerchantIdOrderBySubmissionDateDesc(100L))
                .thenReturn(List.of(sampleRefund));

        List<Refund> result = refundService.getRefundsByMerchantId(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMerchantId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getRefundsByMerchantId: returns empty list for unknown merchant")
    void getRefundsByMerchantId_unknownMerchant_returnsEmpty() {
        when(refundRepository.findByMerchantIdOrderBySubmissionDateDesc(999L))
                .thenReturn(List.of());

        List<Refund> result = refundService.getRefundsByMerchantId(999L);

        assertThat(result).isEmpty();
    }

    // ─── getRefundById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRefundById: found refund is returned")
    void getRefundById_found_returnsRefund() {
        when(refundRepository.findByIdWithMerchant(1L)).thenReturn(Optional.of(sampleRefund));

        Refund result = refundService.getRefundById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getRefundId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getRefundById: throws RuntimeException when not found")
    void getRefundById_notFound_throwsException() {
        when(refundRepository.findByIdWithMerchant(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundService.getRefundById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Refund not found");
    }

    // ─── requestRefund ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("requestRefund: sets status to PENDING and saves")
    void requestRefund_setsPendingAndSaves() {
        Refund input = new Refund();
        input.setMerchantId(100L);
        input.setRefundAmount(new BigDecimal("50.00"));
        input.setCurrency("MYR");

        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> {
            Refund r = inv.getArgument(0);
            r.setRefundId(1L);
            return r;
        });

        Refund result = refundService.requestRefund(input);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(refundRepository).save(input);
    }

    @Test
    @DisplayName("requestRefund: marks linked transaction as REFUND_REQUESTED")
    void requestRefund_withTransactionId_updatesTransactionStatus() {
        sampleRefund.setRefundId(null);
        sampleRefund.setStatus(null);

        when(refundRepository.save(any(Refund.class))).thenReturn(sampleRefund);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        refundService.requestRefund(sampleRefund);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("REFUND_REQUESTED");
    }

    @Test
    @DisplayName("requestRefund: skips transaction update when transactionId is null")
    void requestRefund_noTransactionId_doesNotUpdateTransaction() {
        Refund input = new Refund();
        input.setMerchantId(100L);
        input.setCurrency("MYR");
        // transactionId is null

        when(refundRepository.save(any(Refund.class))).thenReturn(input);

        refundService.requestRefund(input);

        verify(transactionRepository, never()).findById(any());
    }

    // ─── cancelRefund ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelRefund: PENDING refund is cancelled successfully")
    void cancelRefund_pendingRefund_setsCancelled() {
        when(refundRepository.findById(1L)).thenReturn(Optional.of(sampleRefund));
        when(refundRepository.save(any(Refund.class))).thenReturn(sampleRefund);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        Refund result = refundService.cancelRefund(1L);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        verify(refundRepository).save(sampleRefund);
    }

    @Test
    @DisplayName("cancelRefund: sets linked transaction status to PENDING")
    void cancelRefund_setsTransactionToPending() {
        sampleTransaction.setStatus("REFUND_REQUESTED");
        when(refundRepository.findById(1L)).thenReturn(Optional.of(sampleRefund));
        when(refundRepository.save(any(Refund.class))).thenReturn(sampleRefund);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        refundService.cancelRefund(1L);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("cancelRefund: throws when refund is not PENDING")
    void cancelRefund_nonPendingRefund_throwsException() {
        sampleRefund.setStatus("APPROVED");
        when(refundRepository.findById(1L)).thenReturn(Optional.of(sampleRefund));

        assertThatThrownBy(() -> refundService.cancelRefund(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only PENDING refunds can be cancelled");
    }

    @Test
    @DisplayName("cancelRefund: throws when refund not found")
    void cancelRefund_notFound_throwsException() {
        when(refundRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundService.cancelRefund(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Refund not found");
    }

    @Test
    @DisplayName("cancelRefund: skips transaction update when transactionId is null")
    void cancelRefund_noTransactionId_doesNotUpdateTransaction() {
        sampleRefund.setTransactionId(null);
        when(refundRepository.findById(1L)).thenReturn(Optional.of(sampleRefund));
        when(refundRepository.save(any(Refund.class))).thenReturn(sampleRefund);

        refundService.cancelRefund(1L);

        verify(transactionRepository, never()).findById(any());
    }
}
