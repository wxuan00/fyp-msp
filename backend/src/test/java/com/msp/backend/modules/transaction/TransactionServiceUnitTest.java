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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for TransactionService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService — Unit Tests")
class TransactionServiceUnitTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction sampleTxn;

    @BeforeEach
    void setUp() {
        sampleTxn = new Transaction();
        sampleTxn.setTransactionId(1L);
        sampleTxn.setMerchantId(10L);
        sampleTxn.setAmount(new BigDecimal("100.00"));
        sampleTxn.setStatus("PENDING");
        sampleTxn.setCurrency("MYR");
        sampleTxn.setTxnDate(LocalDateTime.now());
        sampleTxn.setCardNo("4111111111111111");
        sampleTxn.setRefNo("REF001");
    }

    @Nested
    @DisplayName("getAllTransactions")
    class GetAllTransactionsTests {

        @Test
        @DisplayName("returns all transactions ordered by txnDate desc")
        void returnsAllOrdered() {
            when(transactionRepository.findAllByOrderByTxnDateDesc()).thenReturn(List.of(sampleTxn));
            List<Transaction> result = transactionService.getAllTransactions();
            assertThat(result).hasSize(1);
            verify(transactionRepository).findAllByOrderByTxnDateDesc();
        }
    }

    @Nested
    @DisplayName("getTransactionsByMerchantId")
    class GetByMerchantTests {

        @Test
        @DisplayName("returns transactions for specific merchant")
        void returnsByMerchant() {
            when(transactionRepository.findByMerchantIdOrderByTxnDateDesc(10L))
                    .thenReturn(List.of(sampleTxn));
            List<Transaction> result = transactionService.getTransactionsByMerchantId(10L);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMerchantId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("getTransactionById")
    class GetByIdTests {

        @Test
        @DisplayName("returns transaction when found")
        void returnsTransaction() {
            when(transactionRepository.findByIdWithMerchant(1L)).thenReturn(Optional.of(sampleTxn));
            Transaction result = transactionService.getTransactionById(1L);
            assertThat(result.getRefNo()).isEqualTo("REF001");
        }

        @Test
        @DisplayName("throws when not found")
        void throwsWhenNotFound() {
            when(transactionRepository.findByIdWithMerchant(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> transactionService.getTransactionById(99L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("searchTransactions (paginated)")
    class SearchTransactionsTests {

        @Test
        @DisplayName("returns paged results with no filters")
        void noFilters() {
            Page<Transaction> page = new PageImpl<>(List.of(sampleTxn));
            when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(page);

            Page<Transaction> result = transactionService.getTransactionsPage(
                    null, null, null, null, null, null, null, null, 0, 10, "txnDate", "desc");
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("returns empty page when no matches")
        void emptyResult() {
            Page<Transaction> emptyPage = new PageImpl<>(List.of());
            when(transactionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            Page<Transaction> result = transactionService.getTransactionsPage(
                    List.of(999L), null, null, null, null, null, null, null, 0, 10, "txnDate", "desc");
            assertThat(result.getContent()).isEmpty();
        }
    }
}
