package com.msp.backend.modules.analytics;

import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.util.MerchantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsController Unit Tests")
class AnalyticsControllerTest {

    @Mock private AnalyticsService analyticsService;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private MerchantRepository merchantRepository;
    @Mock private MerchantResolver merchantResolver;

    @InjectMocks
    private AnalyticsController analyticsController;

    private User adminUser;
    private User merchantUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId(1L);
        adminUser.setEmail("admin@test.com");
        adminUser.setRole("ADMIN");

        merchantUser = new User();
        merchantUser.setUserId(2L);
        merchantUser.setEmail("merchant@test.com");
        merchantUser.setRole("MERCHANT");
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void mockSecurityContext(User user) {
        Authentication auth = mock(Authentication.class);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(user.getEmail());
        SecurityContextHolder.setContext(ctx);

        when(userRepository.findByEmailAndDeletedAtIsNull(user.getEmail()))
                .thenReturn(Optional.of(user));
        doNothing().when(userService).populateRole(user);
    }

    // ── /rfm ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getRfmSegments: admin with no merchantId → fleet-wide (null)")
    void getRfmSegments_adminNoMerchantId_callsFleet() {
        mockSecurityContext(adminUser);
        Map<String, Object> expected = Map.of("totalCustomers", 100);
        when(analyticsService.getRfmSegments(null, null, null)).thenReturn(expected);

        Map<String, Object> result = analyticsController.getRfmSegments(null, null, null);

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getRfmSegments(null, null, null);
    }

    @Test
    @DisplayName("getRfmSegments: admin with merchantId → scoped to that merchant")
    void getRfmSegments_adminWithMerchantId_callsWithMerchantId() {
        mockSecurityContext(adminUser);
        Map<String, Object> expected = Map.of("totalCustomers", 40);
        when(analyticsService.getRfmSegments(5L, null, null)).thenReturn(expected);

        Map<String, Object> result = analyticsController.getRfmSegments(5L, null, null);

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getRfmSegments(5L, null, null);
    }

    @Test
    @DisplayName("getRfmSegments: merchant user ignores merchantId param → uses own merchant")
    void getRfmSegments_merchantUser_usesOwnMerchantId() {
        mockSecurityContext(merchantUser);
        when(merchantResolver.resolveForUser(merchantUser)).thenReturn(3L);
        Map<String, Object> expected = Map.of("totalCustomers", 20);
        when(analyticsService.getRfmSegments(3L, null, null)).thenReturn(expected);

        // Even if an attacker passes a different merchantId, it is ignored for non-admins
        Map<String, Object> result = analyticsController.getRfmSegments(99L, null, null);

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getRfmSegments(3L, null, null);
    }

    @Test
    @DisplayName("getRfmSegments: admin with date range and merchantId")
    void getRfmSegments_adminWithDateRangeAndMerchantId() {
        mockSecurityContext(adminUser);
        Map<String, Object> expected = Map.of("totalCustomers", 15);
        when(analyticsService.getRfmSegments(7L, "2025-01-01", "2025-12-31")).thenReturn(expected);

        Map<String, Object> result = analyticsController.getRfmSegments(7L, "2025-01-01", "2025-12-31");

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getRfmSegments(7L, "2025-01-01", "2025-12-31");
    }

    // ── /churn ────────────────────────────────────────────────────

    @Test
    @DisplayName("getChurnRisk: admin with no merchantId → fleet-wide (null)")
    void getChurnRisk_adminNoMerchantId_callsFleet() {
        mockSecurityContext(adminUser);
        Map<String, Object> expected = Map.of("highRiskCount", 10);
        when(analyticsService.getChurnRisk(null, 90, null, null)).thenReturn(expected);

        Map<String, Object> result = analyticsController.getChurnRisk(null, 90, null, null);

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getChurnRisk(null, 90, null, null);
    }

    @Test
    @DisplayName("getChurnRisk: admin with merchantId → scoped to that merchant")
    void getChurnRisk_adminWithMerchantId_callsWithMerchantId() {
        mockSecurityContext(adminUser);
        Map<String, Object> expected = Map.of("highRiskCount", 3);
        when(analyticsService.getChurnRisk(5L, 90, null, null)).thenReturn(expected);

        Map<String, Object> result = analyticsController.getChurnRisk(5L, 90, null, null);

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getChurnRisk(5L, 90, null, null);
    }

    @Test
    @DisplayName("getChurnRisk: merchant user ignores merchantId param → uses own merchant")
    void getChurnRisk_merchantUser_usesOwnMerchantId() {
        mockSecurityContext(merchantUser);
        when(merchantResolver.resolveForUser(merchantUser)).thenReturn(3L);
        Map<String, Object> expected = Map.of("highRiskCount", 2);
        when(analyticsService.getChurnRisk(3L, 90, null, null)).thenReturn(expected);

        Map<String, Object> result = analyticsController.getChurnRisk(99L, 90, null, null);

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getChurnRisk(3L, 90, null, null);
    }

    @Test
    @DisplayName("getChurnRisk: custom churnDays parameter is forwarded")
    void getChurnRisk_customChurnDays_forwarded() {
        mockSecurityContext(adminUser);
        Map<String, Object> expected = Map.of("highRiskCount", 5);
        when(analyticsService.getChurnRisk(null, 60, null, null)).thenReturn(expected);

        Map<String, Object> result = analyticsController.getChurnRisk(null, 60, null, null);

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getChurnRisk(null, 60, null, null);
    }

    // ── /forecast ─────────────────────────────────────────────────

    @Test
    @DisplayName("getCashFlowForecast: admin with no merchantId → fleet-wide (null)")
    void getCashFlowForecast_adminNoMerchantId_callsFleet() {
        mockSecurityContext(adminUser);
        Map<String, Object> expected = Map.of("totalPredicted", 50000.0);
        when(analyticsService.getCashFlowForecast(null, 30, null, null)).thenReturn(expected);

        Map<String, Object> result = analyticsController.getCashFlowForecast(null, 30, null, null);

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getCashFlowForecast(null, 30, null, null);
    }

    @Test
    @DisplayName("getCashFlowForecast: admin with merchantId → scoped to that merchant")
    void getCashFlowForecast_adminWithMerchantId_callsWithMerchantId() {
        mockSecurityContext(adminUser);
        Map<String, Object> expected = Map.of("totalPredicted", 12000.0);
        when(analyticsService.getCashFlowForecast(5L, 30, null, null)).thenReturn(expected);

        Map<String, Object> result = analyticsController.getCashFlowForecast(5L, 30, null, null);

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getCashFlowForecast(5L, 30, null, null);
    }

    @Test
    @DisplayName("getCashFlowForecast: merchant user ignores merchantId param → uses own merchant")
    void getCashFlowForecast_merchantUser_usesOwnMerchantId() {
        mockSecurityContext(merchantUser);
        when(merchantResolver.resolveForUser(merchantUser)).thenReturn(3L);
        Map<String, Object> expected = Map.of("totalPredicted", 8000.0);
        when(analyticsService.getCashFlowForecast(3L, 30, null, null)).thenReturn(expected);

        Map<String, Object> result = analyticsController.getCashFlowForecast(99L, 30, null, null);

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getCashFlowForecast(3L, 30, null, null);
    }

    @Test
    @DisplayName("getCashFlowForecast: custom horizonDays parameter is forwarded")
    void getCashFlowForecast_customHorizonDays_forwarded() {
        mockSecurityContext(adminUser);
        Map<String, Object> expected = Map.of("totalPredicted", 90000.0);
        when(analyticsService.getCashFlowForecast(null, 90, null, null)).thenReturn(expected);

        Map<String, Object> result = analyticsController.getCashFlowForecast(null, 90, null, null);

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getCashFlowForecast(null, 90, null, null);
    }

    // ── /overview ─────────────────────────────────────────────────

    @Test
    @DisplayName("getOverview: admin → returns fleet overview")
    void getOverview_admin_returnsFleet() {
        mockSecurityContext(adminUser);
        Map<String, Object> expected = Map.of("totalMerchants", 5);
        when(analyticsService.getFleetOverview()).thenReturn(expected);

        Map<String, Object> result = analyticsController.getOverview();

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getFleetOverview();
        verify(analyticsService, never()).getMerchantOverview(any());
    }

    @Test
    @DisplayName("getOverview: merchant → returns own merchant overview")
    void getOverview_merchant_returnsMerchantOverview() {
        mockSecurityContext(merchantUser);
        when(merchantResolver.resolveForUser(merchantUser)).thenReturn(3L);
        Map<String, Object> expected = Map.of("totalSales", 9999.0);
        when(analyticsService.getMerchantOverview(3L)).thenReturn(expected);

        Map<String, Object> result = analyticsController.getOverview();

        assertThat(result).isEqualTo(expected);
        verify(analyticsService).getMerchantOverview(3L);
        verify(analyticsService, never()).getFleetOverview();
    }

    @Test
    @DisplayName("getOverview: merchant with no linked merchant → returns error map")
    void getOverview_merchantNoLinkedMerchant_returnsError() {
        mockSecurityContext(merchantUser);
        when(merchantResolver.resolveForUser(merchantUser)).thenReturn(null);

        Map<String, Object> result = analyticsController.getOverview();

        assertThat(result).containsKey("error");
        verify(analyticsService, never()).getMerchantOverview(any());
    }

    // ── /scorecard ────────────────────────────────────────────────

    @Test
    @DisplayName("getScorecard: admin → returns full scorecard")
    void getScorecard_admin_returnsAll() {
        mockSecurityContext(adminUser);
        List<Map<String, Object>> expected = List.of(
                Map.of("merchantId", 1L, "merchantName", "Merchant A"),
                Map.of("merchantId", 2L, "merchantName", "Merchant B")
        );
        when(analyticsService.getMerchantScorecard()).thenReturn(expected);

        List<Map<String, Object>> result = analyticsController.getScorecard();

        assertThat(result).hasSize(2);
        verify(analyticsService).getMerchantScorecard();
    }

    @Test
    @DisplayName("getScorecard: merchant → returns only own entry")
    void getScorecard_merchant_returnsOwnEntry() {
        mockSecurityContext(merchantUser);
        when(merchantResolver.resolveForUser(merchantUser)).thenReturn(2L);
        List<Map<String, Object>> allScorecard = List.of(
                Map.of("merchantId", 1, "merchantName", "Merchant A"),
                Map.of("merchantId", 2, "merchantName", "Merchant B")
        );
        when(analyticsService.getMerchantScorecard()).thenReturn(allScorecard);

        List<Map<String, Object>> result = analyticsController.getScorecard();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("merchantName")).isEqualTo("Merchant B");
    }
}
