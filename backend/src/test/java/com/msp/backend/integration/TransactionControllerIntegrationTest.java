package com.msp.backend.integration;

import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.transaction.*;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.util.MerchantResolver;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionController — Integration Tests")
class TransactionControllerIntegrationTest {

    @Mock private TransactionService transactionService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private MerchantRepository merchantRepository;
    @Mock private MerchantResolver merchantResolver;

    @InjectMocks private TransactionController transactionController;

    private void mockAdminAuth() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("admin@msp.com");
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        User admin = new User(); admin.setEmail("admin@msp.com"); admin.setRole("ADMIN");
        lenient().when(userRepository.findByEmailAndDeletedAtIsNull("admin@msp.com")).thenReturn(Optional.of(admin));
    }

    @Nested @DisplayName("GET /api/transactions")
    class GetAllTests {

        @Test @DisplayName("admin gets paginated transactions")
        void adminGetsPaged() {
            mockAdminAuth();
            Transaction t = new Transaction(); t.setTransactionId(1L); t.setAmount(new BigDecimal("100"));
            when(transactionService.getTransactionsPage(isNull(), any(), any(), any(), any(), any(), any(), any(),
                    eq(0), eq(10), eq("txnDate"), eq("desc")))
                    .thenReturn(new PageImpl<>(List.of(t)));
            Page<Transaction> result = transactionController.getAllTransactions(
                    0, 10, "txnDate", "desc", null, null, null, null, null, null, null);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test @DisplayName("merchant user with no linked merchants gets empty")
        void merchantNoLinked() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("merchant@msp.com");
            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(ctx);
            User mu = new User(); mu.setEmail("merchant@msp.com"); mu.setRole("MERCHANT");
            when(userRepository.findByEmailAndDeletedAtIsNull("merchant@msp.com")).thenReturn(Optional.of(mu));
            when(merchantResolver.resolveAllForUser(mu)).thenReturn(List.of());
            Page<Transaction> result = transactionController.getAllTransactions(
                    0, 10, "txnDate", "desc", null, null, null, null, null, null, null);
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested @DisplayName("GET /api/transactions/{id}")
    class GetByIdTests {

        @Test @DisplayName("returns transaction for admin")
        void adminGetsById() {
            mockAdminAuth();
            Transaction t = new Transaction(); t.setTransactionId(1L); t.setMerchantId(10L);
            when(transactionService.getTransactionById(1L)).thenReturn(t);
            ResponseEntity<Transaction> resp = transactionController.getTransactionById(1L);
            assertThat(resp.getBody().getTransactionId()).isEqualTo(1L);
        }

        @Test @DisplayName("throws for non-existent transaction")
        void throwsNotFound() {
            mockAdminAuth();
            when(transactionService.getTransactionById(99L)).thenThrow(new RuntimeException("not found"));
            assertThatThrownBy(() -> transactionController.getTransactionById(99L))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
