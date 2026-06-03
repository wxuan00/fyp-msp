package com.msp.backend.modules.analytics;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.settlement.Settlement;
import com.msp.backend.modules.settlement.SettlementService;
import com.msp.backend.modules.transaction.Refund;
import com.msp.backend.modules.transaction.RefundRepository;
import com.msp.backend.modules.transaction.Transaction;
import com.msp.backend.modules.transaction.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to compute, persist, and retrieve analytics data.
 * Stores per-merchant KPIs in the Analytics table and provides
 * advanced analytical computations on-demand.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private static final String SIDECAR_URL = "http://localhost:8000";

    private final AnalyticsRepository analyticsRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionService transactionService;
    private final SettlementService settlementService;
    private final RefundRepository refundRepository;
    private final AIEngine aiEngine;
    private final RestTemplate restTemplate;

    // ===== Analytics Records (persisted to Analytics table) =====

    public List<Analytics> getAnalyticsForMerchant(Long merchantId) {
        return analyticsRepository.findByMerchantIdOrderByGeneratedAtDesc(merchantId);
    }

    public List<Analytics> getAnalyticsByMetric(String dataName) {
        return analyticsRepository.findByDataNameOrderByGeneratedAtDesc(dataName);
    }

    /**
     * Recompute analytics for all merchants and persist to Analytics table.
     */
    public void computeAndStoreAnalytics() {
        List<Merchant> merchants = merchantRepository.findAll();
        for (Merchant merchant : merchants) {
            computeMerchantAnalytics(merchant.getMerchantId());
        }
        log.info("Analytics computed for {} merchants", merchants.size());
    }

    /**
     * Recompute analytics for a specific merchant.
     */
    public void computeMerchantAnalytics(Long merchantId) {
        List<Transaction> txns = transactionService.getTransactionsByMerchantId(merchantId);
        List<Settlement> settlements = settlementService.getSettlementsByMerchantId(merchantId);
        List<Refund> refunds = refundRepository.findByMerchantIdOrderBySubmissionDateDesc(merchantId);

        // Total Sales
        BigDecimal totalSales = txns.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        storeMetric(merchantId, "TOTAL_SALES", totalSales.toPlainString());

        // Total Transactions
        storeMetric(merchantId, "TOTAL_TRANSACTIONS", String.valueOf(txns.size()));

        // Decline Rate
        long declined = txns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count();
        double declineRate = txns.isEmpty() ? 0 : (double) declined / txns.size() * 100;
        storeMetric(merchantId, "DECLINE_RATE", String.format("%.2f", declineRate));

        // Average Transaction Value
        BigDecimal avgTxn = txns.isEmpty() ? BigDecimal.ZERO :
                totalSales.divide(BigDecimal.valueOf(Math.max(1, txns.stream().filter(t -> "APPROVED".equals(t.getStatus())).count())), 2, RoundingMode.HALF_UP);
        storeMetric(merchantId, "AVG_TRANSACTION", avgTxn.toPlainString());

        // Total Settlements
        BigDecimal totalSettled = settlements.stream()
                .map(Settlement::getSettlementAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        storeMetric(merchantId, "TOTAL_SETTLED", totalSettled.toPlainString());

        // Total Refunds
        BigDecimal totalRefunded = refunds.stream()
                .filter(r -> "APPROVED".equals(r.getStatus()) && r.getRefundAmount() != null)
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        storeMetric(merchantId, "TOTAL_REFUNDED", totalRefunded.toPlainString());

        // Refund Rate
        double refundRate = txns.isEmpty() ? 0 : (double) refunds.size() / txns.size() * 100;
        storeMetric(merchantId, "REFUND_RATE", String.format("%.2f", refundRate));

        // Net Revenue
        BigDecimal netRevenue = totalSales.subtract(totalRefunded);
        storeMetric(merchantId, "NET_REVENUE", netRevenue.toPlainString());

        // Payment Channel Breakdown
        Map<String, Long> channels = txns.stream()
                .filter(t -> t.getPaymentChannel() != null)
                .collect(Collectors.groupingBy(Transaction::getPaymentChannel, Collectors.counting()));
        storeMetric(merchantId, "CHANNEL_BREAKDOWN", channels.toString());
    }

    private void storeMetric(Long merchantId, String dataName, String dataValue) {
        Analytics a = new Analytics();
        a.setMerchantId(merchantId);
        a.setDataName(dataName);
        a.setDataValue(dataValue);
        a.setGeneratedAt(LocalDateTime.now());
        analyticsRepository.save(a);
    }

    // ===== On-demand Analytics Computations =====

    /**
     * Get fleet-wide overview: aggregate KPIs across all merchants.
     */
    public Map<String, Object> getFleetOverview() {
        List<Transaction> allTxns = transactionService.getAllTransactions();
        List<Settlement> allSettlements = settlementService.getAllSettlements();
        List<Merchant> allMerchants = merchantRepository.findAll();
        List<Refund> allRefunds = refundRepository.findAll();

        Map<String, Object> overview = new LinkedHashMap<>();

        // Total approved revenue
        BigDecimal totalRevenue = allTxns.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalRevenue", totalRevenue.setScale(2, RoundingMode.HALF_UP));

        // Total nett amount
        BigDecimal totalNett = allTxns.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getNettAmount() != null)
                .map(Transaction::getNettAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalNettRevenue", totalNett.setScale(2, RoundingMode.HALF_UP));

        // Total discount given
        BigDecimal totalDiscount = allTxns.stream()
                .filter(t -> t.getDiscountAmount() != null)
                .map(Transaction::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalDiscount", totalDiscount.setScale(2, RoundingMode.HALF_UP));

        // Total refunded
        BigDecimal totalRefunded = allRefunds.stream()
                .filter(r -> "APPROVED".equals(r.getStatus()) && r.getRefundAmount() != null)
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalRefunded", totalRefunded.setScale(2, RoundingMode.HALF_UP));

        // Net after refunds
        overview.put("netAfterRefunds", totalRevenue.subtract(totalRefunded).setScale(2, RoundingMode.HALF_UP));

        // Counts
        overview.put("totalTransactions", allTxns.size());
        overview.put("approvedTransactions", allTxns.stream().filter(t -> "APPROVED".equals(t.getStatus())).count());
        overview.put("declinedTransactions", allTxns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count());
        overview.put("pendingTransactions", allTxns.stream().filter(t -> "PENDING".equals(t.getStatus())).count());
        overview.put("totalSettlements", allSettlements.size());
        overview.put("totalRefunds", allRefunds.size());
        overview.put("activeMerchants", allMerchants.stream().filter(m -> "ACTIVE".equals(m.getStatus())).count());

        // Decline rate
        double declineRate = allTxns.isEmpty() ? 0 : (double) allTxns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count() / allTxns.size() * 100;
        overview.put("overallDeclineRate", Math.round(declineRate * 100.0) / 100.0);

        // Avg transaction value
        long approvedCount = allTxns.stream().filter(t -> "APPROVED".equals(t.getStatus())).count();
        BigDecimal avgTxn = approvedCount > 0 ? totalRevenue.divide(BigDecimal.valueOf(approvedCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        overview.put("avgTransactionValue", avgTxn);

        // Refund rate
        double refundRate = allTxns.isEmpty() ? 0 : (double) allRefunds.size() / allTxns.size() * 100;
        overview.put("refundRate", Math.round(refundRate * 100.0) / 100.0);

        return overview;
    }

    /**
     * Merchant-level analytics for a specific merchant.
     */
    public Map<String, Object> getMerchantOverview(Long merchantId) {
        List<Transaction> txns = transactionService.getTransactionsByMerchantId(merchantId);
        List<Settlement> settlements = settlementService.getSettlementsByMerchantId(merchantId);
        List<Refund> refunds = refundRepository.findByMerchantIdOrderBySubmissionDateDesc(merchantId);
        Optional<Merchant> merchantOpt = merchantRepository.findById(merchantId);

        Map<String, Object> overview = new LinkedHashMap<>();

        overview.put("merchantId", merchantId);
        merchantOpt.ifPresent(m -> {
            overview.put("merchantName", m.getMerchantName());
            overview.put("status", m.getStatus());
        });

        BigDecimal totalSales = txns.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalSales", totalSales.setScale(2, RoundingMode.HALF_UP));
        overview.put("totalTransactions", txns.size());

        long approved = txns.stream().filter(t -> "APPROVED".equals(t.getStatus())).count();
        long declined = txns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count();
        overview.put("approvedCount", approved);
        overview.put("declinedCount", declined);
        overview.put("declineRate", txns.isEmpty() ? 0 : Math.round((double) declined / txns.size() * 10000.0) / 100.0);

        BigDecimal avgTxn = approved > 0 ? totalSales.divide(BigDecimal.valueOf(approved), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        overview.put("avgTransactionValue", avgTxn);

        BigDecimal totalRefunded = refunds.stream()
                .filter(r -> "APPROVED".equals(r.getStatus()) && r.getRefundAmount() != null)
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalRefunded", totalRefunded.setScale(2, RoundingMode.HALF_UP));
        overview.put("refundCount", refunds.size());
        overview.put("refundRate", txns.isEmpty() ? 0 : Math.round((double) refunds.size() / txns.size() * 10000.0) / 100.0);

        BigDecimal totalSettled = settlements.stream()
                .map(Settlement::getSettlementAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalSettled", totalSettled.setScale(2, RoundingMode.HALF_UP));
        overview.put("settlementCount", settlements.size());

        // Payment channel breakdown
        Map<String, Long> channels = txns.stream()
                .filter(t -> t.getPaymentChannel() != null)
                .collect(Collectors.groupingBy(Transaction::getPaymentChannel, Collectors.counting()));
        overview.put("channelBreakdown", channels);

        return overview;
    }

    /**
     * Trend analysis: period-over-period comparison.
     */
    public Map<String, Object> getTrendAnalysis() {
        List<Transaction> allTxns = transactionService.getAllTransactions();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> trends = new LinkedHashMap<>();

        // ===== Weekly Comparison =====
        List<Transaction> thisWeek = allTxns.stream()
                .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(7)))
                .toList();
        List<Transaction> lastWeek = allTxns.stream()
                .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(14)) && t.getTxnDate().isBefore(now.minusDays(7)))
                .toList();

        trends.put("thisWeekCount", thisWeek.size());
        trends.put("lastWeekCount", lastWeek.size());
        trends.put("weeklyChange", computeChangePercent(lastWeek.size(), thisWeek.size()));

        BigDecimal thisWeekRevenue = sumApproved(thisWeek);
        BigDecimal lastWeekRevenue = sumApproved(lastWeek);
        trends.put("thisWeekRevenue", thisWeekRevenue);
        trends.put("lastWeekRevenue", lastWeekRevenue);
        trends.put("weeklyRevenueChange", computeDecimalChangePercent(lastWeekRevenue, thisWeekRevenue));

        // ===== Monthly Comparison =====
        List<Transaction> thisMonth = allTxns.stream()
                .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(30)))
                .toList();
        List<Transaction> lastMonth = allTxns.stream()
                .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(60)) && t.getTxnDate().isBefore(now.minusDays(30)))
                .toList();

        trends.put("thisMonthCount", thisMonth.size());
        trends.put("lastMonthCount", lastMonth.size());
        trends.put("monthlyChange", computeChangePercent(lastMonth.size(), thisMonth.size()));

        BigDecimal thisMonthRevenue = sumApproved(thisMonth);
        BigDecimal lastMonthRevenue = sumApproved(lastMonth);
        trends.put("thisMonthRevenue", thisMonthRevenue);
        trends.put("lastMonthRevenue", lastMonthRevenue);
        trends.put("monthlyRevenueChange", computeDecimalChangePercent(lastMonthRevenue, thisMonthRevenue));

        // ===== Daily trend (last 30 days) =====
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Long> dailyCounts = new LinkedHashMap<>();
        Map<String, Double> dailyRevenue = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            LocalDateTime day = now.minusDays(i);
            String key = day.format(dayFmt);
            dailyCounts.put(key, 0L);
            dailyRevenue.put(key, 0.0);
        }
        allTxns.stream()
                .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(30)))
                .forEach(t -> {
                    String key = t.getTxnDate().format(dayFmt);
                    dailyCounts.computeIfPresent(key, (k, v) -> v + 1);
                    if ("APPROVED".equals(t.getStatus()) && t.getAmount() != null) {
                        dailyRevenue.computeIfPresent(key, (k, v) -> v + t.getAmount().doubleValue());
                    }
                });
        trends.put("dailyTransactionCounts", dailyCounts);
        trends.put("dailyRevenue", dailyRevenue);

        // ===== Decline rate trend (by week for last 8 weeks) =====
        Map<String, Double> weeklyDeclineRate = new LinkedHashMap<>();
        for (int w = 7; w >= 0; w--) {
            LocalDateTime weekStart = now.minusWeeks(w + 1);
            LocalDateTime weekEnd = now.minusWeeks(w);
            List<Transaction> weekTxns = allTxns.stream()
                    .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(weekStart) && t.getTxnDate().isBefore(weekEnd))
                    .toList();
            long wDeclined = weekTxns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count();
            double rate = weekTxns.isEmpty() ? 0 : (double) wDeclined / weekTxns.size() * 100;
            weeklyDeclineRate.put("W-" + (w + 1), Math.round(rate * 100.0) / 100.0);
        }
        trends.put("weeklyDeclineRate", weeklyDeclineRate);

        return trends;
    }

    /**
     * Merchant performance scorecard: ranks each merchant across KPIs.
     */
    public List<Map<String, Object>> getMerchantScorecard() {
        List<Merchant> merchants = merchantRepository.findAll();
        List<Map<String, Object>> scorecards = new ArrayList<>();

        // Pre-compute fleet averages for benchmarking
        List<Transaction> allTxns = transactionService.getAllTransactions();
        long totalApproved = allTxns.stream().filter(t -> "APPROVED".equals(t.getStatus())).count();
        BigDecimal totalRevenue = sumApproved(allTxns);
        double fleetDeclineRate = allTxns.isEmpty() ? 0 : (double) allTxns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count() / allTxns.size() * 100;
        double fleetAvgTxn = totalApproved > 0 ? totalRevenue.doubleValue() / totalApproved : 0;

        for (Merchant merchant : merchants) {
            List<Transaction> txns = transactionService.getTransactionsByMerchantId(merchant.getMerchantId());
            List<Refund> refunds = refundRepository.findByMerchantIdOrderBySubmissionDateDesc(merchant.getMerchantId());

            Map<String, Object> card = new LinkedHashMap<>();
            card.put("merchantId", merchant.getMerchantId());
            card.put("merchantName", merchant.getMerchantName());
            card.put("status", merchant.getStatus());
            card.put("totalTransactions", txns.size());

            long approved = txns.stream().filter(t -> "APPROVED".equals(t.getStatus())).count();
            long declined = txns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count();

            BigDecimal revenue = sumApproved(txns);
            card.put("revenue", revenue.setScale(2, RoundingMode.HALF_UP));

            double declineRate = txns.isEmpty() ? 0 : (double) declined / txns.size() * 100;
            card.put("declineRate", Math.round(declineRate * 100.0) / 100.0);

            double avgTxn = approved > 0 ? revenue.doubleValue() / approved : 0;
            card.put("avgTransactionValue", BigDecimal.valueOf(avgTxn).setScale(2, RoundingMode.HALF_UP));

            double refundRate = txns.isEmpty() ? 0 : (double) refunds.size() / txns.size() * 100;
            card.put("refundRate", Math.round(refundRate * 100.0) / 100.0);

            // Health score: 0-100 (lower decline & refund rate = better)
            double declinePenalty = Math.min(declineRate / fleetDeclineRate, 2.0) * 30;
            double refundPenalty = Math.min(refundRate / 10.0, 2.0) * 20;
            double volumeBonus = Math.min((double) txns.size() / (allTxns.size() / Math.max(1, merchants.size())), 2.0) * 25;
            double revenueBonus = fleetAvgTxn > 0 ? Math.min(avgTxn / fleetAvgTxn, 2.0) * 25 : 25;
            double healthScore = Math.max(0, Math.min(100, 100 - declinePenalty - refundPenalty + (volumeBonus + revenueBonus - 50)));
            card.put("healthScore", (int) Math.round(healthScore));

            // Rating label
            String rating;
            if (healthScore >= 80) rating = "EXCELLENT";
            else if (healthScore >= 60) rating = "GOOD";
            else if (healthScore >= 40) rating = "FAIR";
            else rating = "AT_RISK";
            card.put("rating", rating);

            scorecards.add(card);
        }

        // Sort by health score descending
        scorecards.sort((a, b) -> Integer.compare((int) b.get("healthScore"), (int) a.get("healthScore")));
        return scorecards;
    }

    /**
     * Anomaly detection: flags unusual patterns across merchants.
     */
    public List<Map<String, Object>> detectAnomalies() {
        List<Merchant> merchants = merchantRepository.findAll();
        List<Transaction> allTxns = transactionService.getAllTransactions();
        List<Map<String, Object>> anomalies = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Fleet averages for comparison
        double avgDailyVolume = allTxns.isEmpty() ? 0 : (double) allTxns.stream()
                .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(30)))
                .count() / 30.0 / Math.max(1, merchants.size());

        for (Merchant merchant : merchants) {
            List<Transaction> txns = transactionService.getTransactionsByMerchantId(merchant.getMerchantId());

            // Check 1: Sudden volume spike (last 3 days vs average)
            long last3Days = txns.stream()
                    .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(3)))
                    .count();
            double recentDailyAvg = last3Days / 3.0;
            long last30Days = txns.stream()
                    .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(30)))
                    .count();
            double monthlyDailyAvg = last30Days / 30.0;

            if (monthlyDailyAvg > 0 && recentDailyAvg > monthlyDailyAvg * 2.5) {
                anomalies.add(createAnomaly(
                        merchant.getMerchantName(),
                        "VOLUME_SPIKE",
                        String.format("Transaction volume spiked %.0f%% in the last 3 days (%.1f/day vs %.1f/day average)", ((recentDailyAvg - monthlyDailyAvg) / monthlyDailyAvg) * 100, recentDailyAvg, monthlyDailyAvg),
                        "warning"
                ));
            }

            // Check 2: High decline rate spike
            List<Transaction> recentTxns = txns.stream()
                    .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(7)))
                    .toList();
            if (recentTxns.size() >= 5) {
                long recentDeclined = recentTxns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count();
                double recentDeclineRate = (double) recentDeclined / recentTxns.size() * 100;
                if (recentDeclineRate > 25) {
                    anomalies.add(createAnomaly(
                            merchant.getMerchantName(),
                            "HIGH_DECLINE",
                            String.format("Decline rate reached %.1f%% in the last 7 days (%d of %d transactions)", recentDeclineRate, recentDeclined, recentTxns.size()),
                            "danger"
                    ));
                }
            }

            // Check 3: Large transaction amounts (potential fraud flag)
            BigDecimal avgAmount = txns.stream()
                    .filter(t -> t.getAmount() != null)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!txns.isEmpty()) {
                avgAmount = avgAmount.divide(BigDecimal.valueOf(txns.size()), 2, RoundingMode.HALF_UP);
            }
            BigDecimal threshold = avgAmount.multiply(BigDecimal.valueOf(5));
            BigDecimal finalAvgAmount = avgAmount;
            txns.stream()
                    .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(7))
                            && t.getAmount() != null && t.getAmount().compareTo(threshold) > 0)
                    .findFirst()
                    .ifPresent(t -> anomalies.add(createAnomaly(
                            merchant.getMerchantName(),
                            "LARGE_TRANSACTION",
                            String.format("Unusually large transaction detected: MYR %s (merchant average: MYR %s)", t.getAmount().toPlainString(), finalAvgAmount.toPlainString()),
                            "warning"
                    )));

            // Check 4: No transactions in recent period (inactive merchant)
            if ("ACTIVE".equals(merchant.getStatus())) {
                long last14Days = txns.stream()
                        .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(14)))
                        .count();
                if (last14Days == 0 && !txns.isEmpty()) {
                    anomalies.add(createAnomaly(
                            merchant.getMerchantName(),
                            "INACTIVE_MERCHANT",
                            "Active merchant has had zero transactions in the last 14 days. May need follow-up.",
                            "info"
                    ));
                }
            }
        }

        // Sort: danger first, then warning, then info
        Map<String, Integer> severityOrder = Map.of("danger", 0, "warning", 1, "info", 2);
        anomalies.sort(Comparator.comparingInt(a -> severityOrder.getOrDefault((String) a.get("severity"), 3)));

        return anomalies;
    }

    /**
     * Revenue breakdown by various dimensions.
     */
    public Map<String, Object> getRevenueBreakdown() {
        List<Transaction> allTxns = transactionService.getAllTransactions();
        Map<String, Object> breakdown = new LinkedHashMap<>();

        // By payment channel
        Map<String, Double> byChannel = allTxns.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getPaymentChannel() != null && t.getAmount() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getPaymentChannel,
                        Collectors.summingDouble(t -> t.getAmount().doubleValue())));
        breakdown.put("byChannel", byChannel);

        // By currency
        Map<String, Double> byCurrency = allTxns.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getCurrency() != null && t.getAmount() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCurrency,
                        Collectors.summingDouble(t -> t.getAmount().doubleValue())));
        breakdown.put("byCurrency", byCurrency);

        // By merchant (top 10)
        Map<String, Double> byMerchant = allTxns.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getMerchantName() != null && t.getAmount() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getMerchantName,
                        Collectors.summingDouble(t -> t.getAmount().doubleValue())));
        Map<String, Double> topMerchantRevenue = byMerchant.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        breakdown.put("byMerchant", topMerchantRevenue);

        // Monthly revenue for last 6 months
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Double> monthlyRevenue = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            String key = now.minusMonths(i).format(monthFmt);
            monthlyRevenue.put(key, 0.0);
        }
        allTxns.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getTxnDate() != null
                        && t.getTxnDate().isAfter(now.minusMonths(6)) && t.getAmount() != null)
                .forEach(t -> {
                    String key = t.getTxnDate().format(monthFmt);
                    monthlyRevenue.computeIfPresent(key, (k, v) -> v + t.getAmount().doubleValue());
                });
        breakdown.put("monthlyRevenue", monthlyRevenue);

        return breakdown;
    }

    // ===== AI Insight Wrappers =====

    public List<Map<String, Object>> getFleetInsights() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        List<Merchant> merchants = merchantRepository.findAll();
        List<Settlement> settlements = settlementService.getAllSettlements();
        List<Map<String, Object>> insights = aiEngine.generateInsights(transactions, merchants, settlements);
        // Enrich with Python model interpretations
        aiEngine.analyzeCustomerSegments(getRfmSegments(null, null, null), insights);
        aiEngine.analyzeChurnRisk(getChurnRisk(null, 90, null, null), insights);
        aiEngine.analyzeCashFlowForecast(getCashFlowForecast(null, 30, null, null), insights);
        return insights;
    }

    public List<Map<String, Object>> getMerchantInsights(Long merchantId) {
        List<Transaction> transactions = transactionService.getTransactionsByMerchantId(merchantId);
        List<Settlement> settlements = settlementService.getSettlementsByMerchantId(merchantId);
        List<Map<String, Object>> insights = aiEngine.generateMerchantInsights(transactions, settlements);
        // Enrich with Python model interpretations scoped to this merchant
        aiEngine.analyzeCustomerSegments(getRfmSegments(merchantId, null, null), insights);
        aiEngine.analyzeChurnRisk(getChurnRisk(merchantId, 90, null, null), insights);
        aiEngine.analyzeCashFlowForecast(getCashFlowForecast(merchantId, 30, null, null), insights);
        return insights;
    }

    // ===== Python Sidecar Proxy Methods =====

    /**
     * Daily scheduled task: recompute KPI analytics for all merchants at 02:00.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledDailyAnalytics() {
        log.info("Scheduled daily analytics recomputation started");
        computeAndStoreAnalytics();
    }

    /**
     * Call the Python sidecar's /rfm endpoint and return the result.
     * @param merchantId optional — null means fleet-wide
     */
    public Map<String, Object> getRfmSegments(Long merchantId, String startDate, String endDate) {
        return callSidecar("/rfm", merchantId, null, null, startDate, endDate);
    }

    /**
     * Call the Python sidecar's /churn endpoint and return the result.
     * @param merchantId optional — null means fleet-wide
     * @param churnDays  days of inactivity that define churn (default 90)
     */
    public Map<String, Object> getChurnRisk(Long merchantId, Integer churnDays, String startDate, String endDate) {
        return callSidecar("/churn", merchantId, churnDays, null, startDate, endDate);
    }

    /**
     * Call the Python sidecar's /forecast endpoint and return the result.
     * @param merchantId   optional — null means fleet-wide
     * @param horizonDays  how many days ahead to forecast (default 30)
     */
    public Map<String, Object> getCashFlowForecast(Long merchantId, Integer horizonDays, String startDate, String endDate) {
        return callSidecar("/forecast", merchantId, null, horizonDays, startDate, endDate);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callSidecar(String path, Long merchantId, Integer churnDays, Integer horizonDays,
                                             String startDate, String endDate) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(SIDECAR_URL + path);
            if (merchantId != null) builder.queryParam("merchant_id", merchantId);
            if (churnDays != null)  builder.queryParam("churn_days", churnDays);
            if (horizonDays != null) builder.queryParam("horizon_days", horizonDays);
            if (startDate != null && !startDate.isBlank()) builder.queryParam("start_date", startDate);
            if (endDate != null && !endDate.isBlank())     builder.queryParam("end_date", endDate);

            return restTemplate.getForObject(builder.toUriString(), Map.class);
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            // Python returned any HTTP error (4xx or 5xx) — extract FastAPI "detail" from body
            log.warn("Python sidecar {} returned {}: {}", path, ex.getStatusCode(), ex.getResponseBodyAsString());
            String detail = ex.getResponseBodyAsString();
            try {
                // Extract "detail" field from FastAPI error JSON without Jackson
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("\"detail\"\\s*:\\s*\"([^\"]+)\"")
                        .matcher(detail);
                if (m.find()) detail = m.group(1);
            } catch (Exception ignored) {}
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", detail);
            return error;
        } catch (Exception ex) {
            log.error("Python sidecar call to {} failed: {}", path, ex.getMessage());
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "AI sidecar unavailable. Please ensure the Python service is running on port 8000.");
            error.put("detail", ex.getMessage());
            return error;
        }
    }

    // ===== Helper Methods =====

    private BigDecimal sumApproved(List<Transaction> txns) {
        return txns.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private double computeChangePercent(long previous, long current) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round(((double) (current - previous) / previous) * 10000.0) / 100.0;
    }

    private double computeDecimalChangePercent(BigDecimal previous, BigDecimal current) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        return Math.round(current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP).doubleValue() * 10000.0) / 100.0;
    }

    private Map<String, Object> createAnomaly(String merchantName, String type, String message, String severity) {
        Map<String, Object> anomaly = new LinkedHashMap<>();
        anomaly.put("merchantName", merchantName);
        anomaly.put("type", type);
        anomaly.put("message", message);
        anomaly.put("severity", severity);
        anomaly.put("detectedAt", LocalDateTime.now().toString());
        return anomaly;
    }
}
