package com.msp.backend.modules.analytics;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.settlement.Settlement;
import com.msp.backend.modules.settlement.SettlementService;
import com.msp.backend.modules.transaction.Transaction;
import com.msp.backend.modules.transaction.TransactionRepository;
import com.msp.backend.modules.transaction.TransactionService;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.util.MerchantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final SettlementService settlementService;
    private final AIEngine aiEngine;
    private final MerchantResolver merchantResolver;

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats(@RequestParam(required = false) Long merchantId,
                                                  @RequestParam(required = false) String startDate,
                                                  @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        Map<String, Object> stats = new HashMap<>();
        final java.time.LocalDate sDate = startDate != null ? java.time.LocalDate.parse(startDate) : null;
        final java.time.LocalDate eDate = endDate != null ? java.time.LocalDate.parse(endDate) : null;

        if ("ADMIN".equals(currentUser.getRole())) {
            if (merchantId != null) {
                // Admin filtering by a specific merchant
                var myMerchant = merchantRepository.findById(merchantId);
                stats.put("totalMerchants", myMerchant.isPresent() ? 1 : 0);
                stats.put("activeMerchants", myMerchant.filter(m -> "ACTIVE".equals(m.getStatus())).isPresent() ? 1 : 0);
                stats.put("pendingMerchants", myMerchant.filter(m -> "PENDING".equals(m.getStatus())).isPresent() ? 1 : 0);
                stats.put("totalTransactions", countTransactionsInRange(transactionRepository.findByMerchantIdOrderByTxnDateDesc(merchantId), sDate, eDate));
                stats.put("totalSettlements", settlementService.getSettlementsByMerchantId(merchantId).size());

                List<User> users = userRepository.findByDeletedAtIsNull();
                users.forEach(userService::populateRole);
                stats.put("totalUsers", users.size());
                stats.put("recentUsers", List.of());
            } else {
                List<User> users = userRepository.findByDeletedAtIsNull();
                users.forEach(userService::populateRole);
                stats.put("totalUsers", users.size());

                List<Map<String, Object>> recentUsers = users.stream()
                        .sorted((a, b) -> {
                            if (a.getCreatedAt() == null) return 1;
                            if (b.getCreatedAt() == null) return -1;
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        })
                        .limit(5)
                        .map(u -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("id", u.getUserId());
                            m.put("firstName", u.getFirstName());
                            m.put("lastName", u.getLastName());
                            m.put("displayName", u.getDisplayName());
                            m.put("email", u.getEmail());
                            m.put("role", u.getRole());
                            m.put("status", u.getStatus());
                            return m;
                        })
                        .toList();
                stats.put("recentUsers", recentUsers);

                var allMerchants = merchantRepository.findAll();
                stats.put("totalMerchants", allMerchants.size());
                stats.put("activeMerchants", allMerchants.stream().filter(m -> "ACTIVE".equals(m.getStatus())).count());
                stats.put("pendingMerchants", allMerchants.stream().filter(m -> "PENDING".equals(m.getStatus())).count());

                stats.put("totalTransactions", countTransactionsInRange(transactionService.getAllTransactions(), sDate, eDate));
                stats.put("totalSettlements", settlementService.getAllSettlements().size());
            }
        } else {
            List<Long> myMerchantIds = merchantResolver.resolveAllForUser(currentUser);
            // If merchantId param given, must be one the user owns
            if (merchantId != null && myMerchantIds.contains(merchantId)) {
                myMerchantIds = List.of(merchantId);
            }
            if (!myMerchantIds.isEmpty()) {
                var myMerchants = merchantRepository.findAllById(myMerchantIds);
                stats.put("totalMerchants", myMerchants.size());
                stats.put("activeMerchants", myMerchants.stream().filter(m -> "ACTIVE".equals(m.getStatus())).count());
                stats.put("pendingMerchants", myMerchants.stream().filter(m -> "PENDING".equals(m.getStatus())).count());

                long txnCount = 0;
                long settlCount = 0;
                for (Long mid : myMerchantIds) {
                    txnCount += countTransactionsInRange(transactionRepository.findByMerchantIdOrderByTxnDateDesc(mid), sDate, eDate);
                    settlCount += settlementService.getSettlementsByMerchantId(mid).size();
                }
                stats.put("totalTransactions", txnCount);
                stats.put("totalSettlements", settlCount);
            } else {
                stats.put("totalMerchants", 0);
                stats.put("activeMerchants", 0);
                stats.put("pendingMerchants", 0);
                stats.put("totalTransactions", 0);
                stats.put("totalSettlements", 0);
            }
        }

        return stats;
    }

    private long countTransactionsInRange(List<Transaction> txns, java.time.LocalDate sDate, java.time.LocalDate eDate) {
        return txns.stream().filter(t -> {
            if (t.getTxnDate() == null) return false;
            java.time.LocalDate d = t.getTxnDate().toLocalDate();
            if (sDate != null && d.isBefore(sDate)) return false;
            if (eDate != null && d.isAfter(eDate)) return false;
            return true;
        }).count();
    }

    @GetMapping("/insights")
    public List<Map<String, Object>> getInsights() {
        User currentUser = getCurrentUser();

        if ("ADMIN".equals(currentUser.getRole())) {
            List<Transaction> transactions = transactionService.getAllTransactions();
            List<Merchant> merchants = merchantRepository.findAll();
            List<Settlement> settlements = settlementService.getAllSettlements();
            return aiEngine.generateInsights(transactions, merchants, settlements);
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null) return List.of();

            List<Transaction> transactions = transactionService.getTransactionsByMerchantId(merchantId);
            List<Settlement> settlements = settlementService.getSettlementsByMerchantId(merchantId);
            return aiEngine.generateMerchantInsights(transactions, settlements);
        }
    }

    @GetMapping("/chart-data")
    public Map<String, Object> getChartData(@RequestParam(required = false) Long merchantId,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        Map<String, Object> charts = new HashMap<>();
        final java.time.LocalDate sDate = startDate != null ? java.time.LocalDate.parse(startDate) : null;
        final java.time.LocalDate eDate = endDate != null ? java.time.LocalDate.parse(endDate) : null;

        List<Transaction> transactions;
        List<Settlement> settlements;
        List<Merchant> merchants;

        if ("ADMIN".equals(currentUser.getRole())) {
            if (merchantId != null) {
                transactions = transactionService.getTransactionsByMerchantId(merchantId);
                settlements = settlementService.getSettlementsByMerchantId(merchantId);
                merchants = merchantRepository.findById(merchantId).map(List::of).orElse(List.of());
            } else {
                transactions = transactionService.getAllTransactions();
                settlements = settlementService.getAllSettlements();
                merchants = merchantRepository.findAll();
            }
        } else {
            List<Long> myMerchantIds = merchantResolver.resolveAllForUser(currentUser);
            if (merchantId != null && myMerchantIds.contains(merchantId)) {
                myMerchantIds = List.of(merchantId);
            }
            transactions = new java.util.ArrayList<>();
            settlements = new java.util.ArrayList<>();
            for (Long mid : myMerchantIds) {
                transactions.addAll(transactionService.getTransactionsByMerchantId(mid));
                settlements.addAll(settlementService.getSettlementsByMerchantId(mid));
            }
            merchants = merchantRepository.findAllById(myMerchantIds);
        }

        // Apply date range filter
        if (sDate != null) {
            transactions = transactions.stream().filter(t -> t.getTxnDate() != null && !t.getTxnDate().toLocalDate().isBefore(sDate)).collect(Collectors.toList());
        }
        if (eDate != null) {
            transactions = transactions.stream().filter(t -> t.getTxnDate() != null && !t.getTxnDate().toLocalDate().isAfter(eDate)).collect(Collectors.toList());
        }

        // 1. Transaction Status Breakdown (Doughnut)
        Map<String, Long> txnStatus = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus() != null ? t.getStatus() : "UNKNOWN",
                        Collectors.counting()));
        charts.put("transactionStatus", txnStatus);

        // 2. Transaction Volume by Day (dynamic range - Line)
        final java.time.LocalDate volumeStart;
        final java.time.LocalDate volumeEnd;
        if (sDate != null && eDate != null) {
            volumeStart = sDate;
            volumeEnd = eDate;
        } else if (sDate != null) {
            volumeStart = sDate;
            volumeEnd = java.time.LocalDate.now();
        } else if (eDate != null) {
            volumeStart = eDate.minusDays(6);
            volumeEnd = eDate;
        } else {
            volumeStart = java.time.LocalDate.now().minusDays(6);
            volumeEnd = java.time.LocalDate.now();
        }
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MMM dd");
        Map<String, Long> dailyVolume = new LinkedHashMap<>();
        for (java.time.LocalDate d = volumeStart; !d.isAfter(volumeEnd); d = d.plusDays(1)) {
            dailyVolume.put(d.format(dayFmt), 0L);
        }
        transactions.stream()
                .filter(t -> t.getTxnDate() != null
                        && !t.getTxnDate().toLocalDate().isBefore(volumeStart)
                        && !t.getTxnDate().toLocalDate().isAfter(volumeEnd))
                .forEach(t -> {
                    String key = t.getTxnDate().toLocalDate().format(dayFmt);
                    dailyVolume.computeIfPresent(key, (k, v) -> v + 1);
                });
        charts.put("dailyTransactionVolume", dailyVolume);

        // 3. Payment Channel Distribution (Doughnut)
        Map<String, Long> channelDist = transactions.stream()
                .filter(t -> t.getPaymentChannel() != null && !t.getPaymentChannel().isEmpty())
                .collect(Collectors.groupingBy(Transaction::getPaymentChannel, Collectors.counting()));
        charts.put("paymentChannelDistribution", channelDist);

        // 4. Settlement Type Breakdown (Doughnut)
        Map<String, Long> settlementTypes = settlements.stream()
                .filter(s -> s.getSettlementType() != null)
                .collect(Collectors.groupingBy(Settlement::getSettlementType, Collectors.counting()));
        charts.put("settlementTypes", settlementTypes);

        // 5. Daily Revenue (MYR) – dynamic range (Bar)
        Map<String, Double> dailyRevenue = new LinkedHashMap<>();
        for (java.time.LocalDate d = volumeStart; !d.isAfter(volumeEnd); d = d.plusDays(1)) {
            dailyRevenue.put(d.format(dayFmt), 0.0);
        }
        transactions.stream()
                .filter(t -> t.getTxnDate() != null
                        && !t.getTxnDate().toLocalDate().isBefore(volumeStart)
                        && !t.getTxnDate().toLocalDate().isAfter(volumeEnd)
                        && t.getAmount() != null)
                .forEach(t -> {
                    String key = t.getTxnDate().toLocalDate().format(dayFmt);
                    dailyRevenue.computeIfPresent(key, (k, v) -> v + t.getAmount().doubleValue());
                });
        charts.put("dailyRevenue", dailyRevenue);

        // 6. Top 5 Merchants by Transaction Volume (Bar) - Admin only
        if ("ADMIN".equals(currentUser.getRole())) {
            Map<String, Long> merchantVolume = transactions.stream()
                    .filter(t -> t.getMerchantName() != null)
                    .collect(Collectors.groupingBy(Transaction::getMerchantName, Collectors.counting()));
            Map<String, Long> topMerchants = merchantVolume.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
            charts.put("topMerchants", topMerchants);

            // 7. Merchant Status Distribution (Doughnut) - Admin only
            Map<String, Long> merchantStatus = merchants.stream()
                    .collect(Collectors.groupingBy(
                            m -> m.getStatus() != null ? m.getStatus() : "UNKNOWN",
                            Collectors.counting()));
            charts.put("merchantStatus", merchantStatus);
        }

        return charts;
    }

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
