package com.msp.backend.modules.analytics;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.util.MerchantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantRepository merchantRepository;
    private final MerchantResolver merchantResolver;

    /**
     * Fleet-wide overview (admin) or merchant-specific overview.
     */
    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return analyticsService.getFleetOverview();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null) return Map.of("error", "No merchant linked to your account");
            return analyticsService.getMerchantOverview(merchantId);
        }
    }

    /**
     * Trend analysis: period-over-period comparison.
     */
    @GetMapping("/trends")
    public Map<String, Object> getTrends() {
        return analyticsService.getTrendAnalysis();
    }

    /**
     * Merchant performance scorecard (admin only shows all; merchant sees own).
     */
    @GetMapping("/scorecard")
    public List<Map<String, Object>> getScorecard() {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return analyticsService.getMerchantScorecard();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null) return List.of();
            return analyticsService.getMerchantScorecard().stream()
                    .filter(s -> merchantId.equals(((Number) s.get("merchantId")).longValue()))
                    .toList();
        }
    }

    /**
     * Anomaly detection alerts.
     */
    @GetMapping("/anomalies")
    public List<Map<String, Object>> getAnomalies() {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return analyticsService.detectAnomalies();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null) return List.of();
            String merchantName = merchantRepository.findById(merchantId).map(Merchant::getMerchantName).orElse("");
            return analyticsService.detectAnomalies().stream()
                    .filter(a -> merchantName.equals(a.get("merchantName")))
                    .toList();
        }
    }

    /**
     * Revenue breakdown by various dimensions.
     */
    @GetMapping("/revenue")
    public Map<String, Object> getRevenueBreakdown() {
        return analyticsService.getRevenueBreakdown();
    }

    /**
     * AI-powered insights (delegates to existing AIEngine via DashboardController logic).
     */
    @GetMapping("/insights")
    public List<Map<String, Object>> getInsights() {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return analyticsService.getFleetInsights();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null) return List.of();
            return analyticsService.getMerchantInsights(merchantId);
        }
    }

    /**
     * Trigger analytics recomputation (admin only).
     */
    @PostMapping("/recompute")
    public ResponseEntity<Map<String, String>> recomputeAnalytics() {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin only"));
        }
        analyticsService.computeAndStoreAnalytics();
        return ResponseEntity.ok(Map.of("message", "Analytics recomputed successfully"));
    }

    /**
     * Get stored analytics records for a specific merchant (admin or own).
     */
    @GetMapping("/records/{merchantId}")
    public List<Analytics> getMerchantRecords(@PathVariable Long merchantId) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) {
            Long myMerchantId = getMyMerchantId(currentUser);
            if (myMerchantId == null || !myMerchantId.equals(merchantId)) {
                return List.of();
            }
        }
        return analyticsService.getAnalyticsForMerchant(merchantId);
    }

    // ===== AI Model Endpoints (Python Sidecar Proxy) =====

    /**
     * RFM customer segmentation via K-Means (Python sidecar).
     * Admin sees fleet-wide; merchant sees own data.
     */
    @GetMapping("/rfm")
    public Map<String, Object> getRfmSegments(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        Long resolvedMerchantId;
        if ("ADMIN".equals(currentUser.getRole())) {
            resolvedMerchantId = merchantId; // null = fleet-wide, non-null = specific merchant
        } else {
            resolvedMerchantId = getMyMerchantId(currentUser);
        }
        return analyticsService.getRfmSegments(resolvedMerchantId, startDate, endDate);
    }

    /**
     * Customer churn prediction via XGBoost (Python sidecar).
     * @param churnDays days of inactivity that defines churn (default 90)
     */
    @GetMapping("/churn")
    public Map<String, Object> getChurnRisk(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(defaultValue = "90") int churnDays,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        Long resolvedMerchantId;
        if ("ADMIN".equals(currentUser.getRole())) {
            resolvedMerchantId = merchantId;
        } else {
            resolvedMerchantId = getMyMerchantId(currentUser);
        }
        return analyticsService.getChurnRisk(resolvedMerchantId, churnDays, startDate, endDate);
    }

    /**
     * Daily cash-flow forecast via Prophet (Python sidecar).
     * @param horizonDays how many days ahead to forecast (default 30)
     */
    @GetMapping("/forecast")
    public Map<String, Object> getCashFlowForecast(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(defaultValue = "30") int horizonDays,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        Long resolvedMerchantId;
        if ("ADMIN".equals(currentUser.getRole())) {
            resolvedMerchantId = merchantId;
        } else {
            resolvedMerchantId = getMyMerchantId(currentUser);
        }
        return analyticsService.getCashFlowForecast(resolvedMerchantId, horizonDays, startDate, endDate);
    }

    // ===== Helpers =====

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
        userService.populateRole(user);
        return user;
    }

    private Long getMyMerchantId(User user) {
        return merchantResolver.resolveForUser(user);
    }
}
