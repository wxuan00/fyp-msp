package com.msp.backend.integration;

import com.msp.backend.modules.settlement.*;
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
@DisplayName("SettlementController — Integration Tests")
class SettlementControllerIntegrationTest {

    @Mock private SettlementService settlementService;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private MerchantResolver merchantResolver;

    @InjectMocks private SettlementController settlementController;

    private void mockAdminAuth() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("admin@msp.com");
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        User admin = new User(); admin.setEmail("admin@msp.com"); admin.setRole("ADMIN");
        lenient().when(userRepository.findByEmailAndDeletedAtIsNull("admin@msp.com")).thenReturn(Optional.of(admin));
    }

    @Nested @DisplayName("GET /api/settlements")
    class GetAllTests {

        @Test @DisplayName("admin gets paginated settlements")
        void adminGetsPaged() {
            mockAdminAuth();
            Settlement s = new Settlement(); s.setSettlementId(1L); s.setSettlementAmount(new BigDecimal("5000"));
            when(settlementService.getSettlementsPage(any(), any(), any(), any(), any(), any(),
                    eq(0), eq(10), eq("settlementDate"), eq("desc")))
                    .thenReturn(new PageImpl<>(List.of(s)));
            Page<Settlement> result = settlementController.getAllSettlements(
                    0, 10, "settlementDate", "desc", null, null, null, null, null);
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested @DisplayName("GET /api/settlements/{id}")
    class GetByIdTests {

        @Test @DisplayName("returns settlement for admin")
        void adminGetsById() {
            mockAdminAuth();
            Settlement s = new Settlement(); s.setSettlementId(1L);
            when(settlementService.getSettlementById(1L)).thenReturn(s);
            ResponseEntity<Settlement> resp = settlementController.getSettlementById(1L);
            assertThat(resp.getBody().getSettlementId()).isEqualTo(1L);
        }

        @Test @DisplayName("throws for non-existent settlement")
        void throwsNotFound() {
            mockAdminAuth();
            when(settlementService.getSettlementById(99L)).thenThrow(new RuntimeException("not found"));
            assertThatThrownBy(() -> settlementController.getSettlementById(99L))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
