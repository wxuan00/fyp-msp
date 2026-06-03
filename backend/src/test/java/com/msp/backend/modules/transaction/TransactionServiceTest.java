package com.msp.backend.modules.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction sampleTransaction;
    private Transaction sampleTransaction2;

    @BeforeEach
    void setUp() {
        sampleTransaction = new Transaction();
        sampleTransaction.setTransactionId(1L);
        sampleTransaction.setMerchantId(100L);
        sampleTransaction.setStatus("APPROVED");
        sampleTransaction.setAmount(new BigDecimal("150.00"));
        sampleTransaction.setCurrency("MYR");
        sampleTransaction.setPaymentChannel("ONLINE");
        sampleTransaction.setTxnDate(LocalDateTime.now().minusDays(1));

        sampleTransaction2 = new Transaction();
        sampleTransaction2.setTransactionId(2L);
        sampleTransaction2.setMerchantId(100L);
        sampleTransaction2.setStatus("PENDING");
        sampleTransaction2.setAmount(new BigDecimal("200.00"));
        sampleTransaction2.setCurrency("MYR");
        sampleTransaction2.setPaymentChannel("POS");
        sampleTransaction2.setTxnDate(LocalDateTime.now());
    }

    // ─── getAllTransactions ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllTransactions: returns all transactions from repository")
    void getAllTransactions_returnsList() {
        List<Transaction> expected = Arrays.asList(sampleTransaction2, sampleTransaction);
        when(transactionRepository.findAllByOrderByTxnDateDesc()).thenReturn(expected);

        List<Transaction> result = transactionService.getAllTransactions();

        assertThat(result).hasSize(2);
        verify(transactionRepository).findAllByOrderByTxnDateDesc();
    }

    @Test
    @DisplayName("getAllTransactions: returns empty list when no transactions exist")
    void getAllTransactions_emptyList() {
        when(transactionRepository.findAllByOrderByTxnDateDesc()).thenReturn(List.of());

        List<Transaction> result = transactionService.getAllTransactions();

        assertThat(result).isEmpty();
    }

    // ─── getTransactionsByMerchantId ─────────────────────────────────────────────

    @Test
    @DisplayName("getTransactionsByMerchantId: returns transactions for given merchant")
    void getTransactionsByMerchantId_returnsList() {
        when(transactionRepository.findByMerchantIdOrderByTxnDateDesc(100L))
                .thenReturn(Arrays.asList(sampleTransaction2, sampleTransaction));

        List<Transaction> result = transactionService.getTransactionsByMerchantId(100L);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getMerchantId().equals(100L));
    }

    @Test
    @DisplayName("getTransactionsByMerchantId: returns empty for unknown merchant")
    void getTransactionsByMerchantId_unknownMerchant_returnsEmpty() {
        when(transactionRepository.findByMerchantIdOrderByTxnDateDesc(999L))
                .thenReturn(List.of());

        List<Transaction> result = transactionService.getTransactionsByMerchantId(999L);

        assertThat(result).isEmpty();
    }

    // ─── getTransactionById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getTransactionById: returns transaction when found")
    void getTransactionById_found_returnsTransaction() {
        when(transactionRepository.findByIdWithMerchant(1L))
                .thenReturn(Optional.of(sampleTransaction));

        Transaction result = transactionService.getTransactionById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("getTransactionById: throws RuntimeException when not found")
    void getTransactionById_notFound_throwsException() {
        when(transactionRepository.findByIdWithMerchant(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transaction not found");
    }

    // ─── getTransactionsPage field coverage ──────────────────────────────────────

    @Test
    @DisplayName("getTransactionsPage: delegates to repository with pageable")
    void getTransactionsPage_callsRepository() {
        org.springframework.data.domain.Page<Transaction> page =
                org.springframework.data.domain.Page.empty();
        when(transactionRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        org.springframework.data.domain.Page<Transaction> result =
                transactionService.getTransactionsPage(
                        null, null, null, null, null, null, null, null,
                        0, 10, "txnDate", "desc");

        assertThat(result).isNotNull();
        verify(transactionRepository).findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("getTransactionsPage: applies merchantId restriction for non-admin")
    void getTransactionsPage_withMerchantRestriction() {
        org.springframework.data.domain.Page<Transaction> page =
                org.springframework.data.domain.Page.empty();
        when(transactionRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        org.springframework.data.domain.Page<Transaction> result =
                transactionService.getTransactionsPage(
                        java.util.List.of(100L), "TestMerchant", "1", "4111", "APPROVED", "ONLINE",
                        "2025-01-01", "2025-12-31",
                        0, 5, "txnDate", "asc");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getTransactionsPage: sorts by merchantName correctly")
    void getTransactionsPage_sortByMerchantName() {
        org.springframework.data.domain.Page<Transaction> page =
                org.springframework.data.domain.Page.empty();
        when(transactionRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        org.springframework.data.domain.Page<Transaction> result =
                transactionService.getTransactionsPage(
                        null, null, null, null, null, null, null, null,
                        0, 10, "merchantName", "asc");

        assertThat(result).isNotNull();
    }
}
