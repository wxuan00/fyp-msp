package com.msp.backend.modules.analytics;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.settlement.Settlement;
import com.msp.backend.modules.transaction.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Analytics Engine - Generates smart insights from transaction,
 * merchant, and settlement data using rule-based heuristics.
 */
@Component
public class AIEngine {

    public List<Map<String, Object>> generateInsights(
            List<Transaction> transactions,
            List<Merchant> merchants,
            List<Settlement> settlements
    ) {
        List<Map<String, Object>> insights = new ArrayList<>();

        analyzeTransactionVolume(transactions, insights);
        analyzeDeclineRate(transactions, insights);
        analyzeTopMerchants(transactions, insights);
        analyzeSettlementSummary(settlements, insights);
        analyzePaymentChannels(transactions, insights);
        analyzeMerchantRisk(transactions, merchants, insights);
        analyzeRevenue(transactions, insights);

        return insights;
    }

    public List<Map<String, Object>> generateMerchantInsights(
            List<Transaction> transactions,
            List<Settlement> settlements
    ) {
        List<Map<String, Object>> insights = new ArrayList<>();

        analyzeTransactionVolume(transactions, insights);
        analyzeDeclineRate(transactions, insights);
        analyzePaymentChannels(transactions, insights);
        analyzeSettlementSummary(settlements, insights);
        analyzeRevenue(transactions, insights);

        return insights;
    }

    // ----- Individual analysis methods -----

    private void analyzeTransactionVolume(List<Transaction> transactions, List<Map<String, Object>> insights) {
        if (transactions.isEmpty()) return;

        long total = transactions.size();
        LocalDateTime now = LocalDateTime.now();
        long last7Days = transactions.stream()
                .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(7)))
                .count();
        long last30Days = transactions.stream()
                .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(30)))
                .count();

        String trend;
        String severity;
        if (last7Days > 0 && last30Days > 0) {
            double weeklyRate = (double) last7Days / 7;
            double monthlyRate = (double) last30Days / 30;
            double change = ((weeklyRate - monthlyRate) / monthlyRate) * 100;

            if (change > 20) {
                trend = String.format("Transaction volume is UP %.0f%% this week vs monthly average (%.0f/day vs %.0f/day). Growth is accelerating.", change, weeklyRate, monthlyRate);
                severity = "success";
            } else if (change < -20) {
                trend = String.format("Transaction volume is DOWN %.0f%% this week vs monthly average. Consider investigating.", Math.abs(change));
                severity = "warning";
            } else {
                trend = String.format("Transaction volume is STABLE with ~%.0f transactions/day average.", monthlyRate);
                severity = "info";
            }
        } else {
            trend = String.format("Total of %d transactions recorded.", total);
            severity = "info";
        }

        insights.add(createInsight("Transaction Volume", trend, severity, "volume"));
    }

    private void analyzeDeclineRate(List<Transaction> transactions, List<Map<String, Object>> insights) {
        if (transactions.isEmpty()) return;

        long total = transactions.size();
        long declined = transactions.stream().filter(t -> "DECLINED".equals(t.getStatus())).count();
        double declineRate = (double) declined / total * 100;

        String message;
        String severity;
        if (declineRate > 10) {
            message = String.format("Decline rate is HIGH at %.1f%% (%d of %d transactions). This may indicate issues with payment processing or fraud risk.", declineRate, declined, total);
            severity = "danger";
        } else if (declineRate > 5) {
            message = String.format("Decline rate is MODERATE at %.1f%% (%d declined). Monitor for any increasing trends.", declineRate, declined);
            severity = "warning";
        } else {
            message = String.format("Decline rate is HEALTHY at %.1f%% (%d of %d transactions). Well within acceptable range.", declineRate, declined, total);
            severity = "success";
        }

        insights.add(createInsight("Decline Rate Analysis", message, severity, "decline"));
    }

    private void analyzeTopMerchants(List<Transaction> transactions, List<Map<String, Object>> insights) {
        if (transactions.isEmpty()) return;

        Map<String, Long> merchantCounts = transactions.stream()
                .filter(t -> t.getMerchantName() != null)
                .collect(Collectors.groupingBy(Transaction::getMerchantName, Collectors.counting()));

        List<Map.Entry<String, Long>> top = merchantCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .toList();

        if (!top.isEmpty()) {
            StringBuilder sb = new StringBuilder("Top merchants by transaction count: ");
            for (int i = 0; i < top.size(); i++) {
                Map.Entry<String, Long> entry = top.get(i);
                sb.append(String.format("%d) %s (%d txns)", i + 1, entry.getKey(), entry.getValue()));
                if (i < top.size() - 1) sb.append(", ");
            }
            insights.add(createInsight("Top Merchants", sb.toString(), "info", "top-merchants"));
        }
    }

    private void analyzeSettlementSummary(List<Settlement> settlements, List<Map<String, Object>> insights) {
        if (settlements.isEmpty()) return;

        long total = settlements.size();

        Map<String, Long> byType = settlements.stream()
                .filter(s -> s.getSettlementType() != null)
                .collect(Collectors.groupingBy(Settlement::getSettlementType, Collectors.counting()));

        BigDecimal totalAmount = settlements.stream()
                .map(Settlement::getSettlementAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total %d settlements worth %s. ", total, totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()));

        if (!byType.isEmpty()) {
            sb.append("Breakdown by type: ");
            byType.forEach((type, count) -> sb.append(String.format("%s: %d, ", type, count)));
        }

        String message = sb.toString().replaceAll(", $", "");
        insights.add(createInsight("Settlement Summary", message, "info", "settlement"));
    }

    private void analyzePaymentChannels(List<Transaction> transactions, List<Map<String, Object>> insights) {
        if (transactions.isEmpty()) return;

        Map<String, Long> channelCounts = transactions.stream()
                .filter(t -> t.getPaymentChannel() != null && !t.getPaymentChannel().isEmpty())
                .collect(Collectors.groupingBy(Transaction::getPaymentChannel, Collectors.counting()));

        if (channelCounts.isEmpty()) return;

        long total = channelCounts.values().stream().mapToLong(Long::longValue).sum();
        StringBuilder sb = new StringBuilder("Payment channel distribution: ");
        channelCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> sb.append(String.format("%s %.0f%%, ", e.getKey(), (double) e.getValue() / total * 100)));

        insights.add(createInsight("Payment Channels", sb.toString().replaceAll(", $", ""), "info", "channels"));
    }

    private void analyzeMerchantRisk(List<Transaction> transactions, List<Merchant> merchants, List<Map<String, Object>> insights) {
        if (transactions.isEmpty() || merchants.isEmpty()) return;

        Map<Long, List<Transaction>> byMerchant = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getMerchantId));

        List<String> riskyMerchants = new ArrayList<>();
        for (Map.Entry<Long, List<Transaction>> entry : byMerchant.entrySet()) {
            List<Transaction> txns = entry.getValue();
            long declined = txns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count();
            double rate = (double) declined / txns.size() * 100;
            if (rate > 15 && txns.size() >= 5) {
                String name = txns.get(0).getMerchantName();
                riskyMerchants.add(String.format("%s (%.0f%% decline)", name != null ? name : "ID:" + entry.getKey(), rate));
            }
        }

        if (!riskyMerchants.isEmpty()) {
            String message = "High-risk merchants detected with decline rates >15%: " + String.join(", ", riskyMerchants) + ". Consider reviewing their accounts.";
            insights.add(createInsight("Risk Alert", message, "danger", "risk"));
        } else {
            insights.add(createInsight("Risk Assessment", "No merchants currently flagged as high-risk. All decline rates are within acceptable limits.", "success", "risk"));
        }
    }

    private void analyzeRevenue(List<Transaction> transactions, List<Map<String, Object>> insights) {
        if (transactions.isEmpty()) return;

        BigDecimal totalApproved = transactions.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNett = transactions.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getNettAmount() != null)
                .map(Transaction::getNettAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = transactions.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getDiscountAmount() != null)
                .map(Transaction::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String message = String.format(
                "Total approved amount: MYR %s | Total discount: MYR %s | Net amount: MYR %s",
                totalApproved.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                totalDiscount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                totalNett.setScale(2, RoundingMode.HALF_UP).toPlainString()
        );

        insights.add(createInsight("Revenue Summary", message, "info", "revenue"));
    }

    private Map<String, Object> createInsight(String title, String message, String severity, String category) {
        Map<String, Object> insight = new HashMap<>();
        insight.put("title", title);
        insight.put("message", message);
        insight.put("severity", severity);
        insight.put("category", category);
        insight.put("generatedAt", LocalDateTime.now().toString());
        return insight;
    }

    // ===== AI Model Result Interpreters =====

    /**
     * Interpret Python sidecar RFM segmentation results into human-readable insights.
     */
    @SuppressWarnings("unchecked")
    public void analyzeCustomerSegments(Map<String, Object> rfmResult, List<Map<String, Object>> insights) {
        if (rfmResult == null || rfmResult.containsKey("error")) {
            insights.add(createInsight("Customer Segmentation", "RFM segmentation data unavailable — ensure the AI sidecar is running.", "warning", "segmentation"));
            return;
        }

        List<Map<String, Object>> summary = (List<Map<String, Object>>) rfmResult.get("clusterSummary");
        if (summary == null || summary.isEmpty()) return;

        int totalCustomers = rfmResult.containsKey("totalCustomers") ? (int) rfmResult.get("totalCustomers") : 0;

        // Count champions (lowest recency cluster)
        Map<String, Object> champions = summary.stream()
                .filter(c -> "Champions".equals(c.get("label")))
                .findFirst().orElse(null);
        Map<String, Object> atRisk = summary.stream()
                .filter(c -> "At Risk".equals(c.get("label")))
                .findFirst().orElse(null);

        if (champions != null) {
            int count = (int) champions.get("count");
            double pct = totalCustomers > 0 ? (double) count / totalCustomers * 100 : 0;
            String msg = String.format(
                    "You have %d 'Champion' customers (%.0f%% of base) — high frequency, recent buyers. Consider a loyalty reward or VIP tier to retain them.",
                    count, pct);
            insights.add(createInsight("Champion Customers 🏆", msg, "success", "segmentation"));
        }

        if (atRisk != null) {
            int count = (int) atRisk.get("count");
            double pct = totalCustomers > 0 ? (double) count / totalCustomers * 100 : 0;
            String msg = String.format(
                    "%d customers (%.0f%% of base) are classified 'At Risk' — previously active but showing reduced engagement. A targeted re-engagement campaign is recommended.",
                    count, pct);
            insights.add(createInsight("At-Risk Customers ⚠️", msg, "warning", "segmentation"));
        }

        Double silhouette = rfmResult.containsKey("silhouetteScore") && rfmResult.get("silhouetteScore") != null
                ? ((Number) rfmResult.get("silhouetteScore")).doubleValue() : null;
        String quality = silhouette == null ? "" : silhouette > 0.5 ? " (model quality: good)" : silhouette > 0.3 ? " (model quality: fair)" : " (model quality: weak — more data improves this)";
        insights.add(createInsight("RFM Segmentation Model" + quality, String.format("Segmented %d unique customers into %d clusters using K-Means RFM analysis.", totalCustomers, summary.size()), "info", "segmentation"));
    }

    /**
     * Interpret Python sidecar XGBoost churn prediction results into insights.
     */
    @SuppressWarnings("unchecked")
    public void analyzeChurnRisk(Map<String, Object> churnResult, List<Map<String, Object>> insights) {
        if (churnResult == null || churnResult.containsKey("error")) {
            insights.add(createInsight("Churn Prediction", "Churn prediction unavailable — ensure the AI sidecar is running.", "warning", "churn"));
            return;
        }

        int highRiskCount = churnResult.containsKey("highRiskCount") ? (int) churnResult.get("highRiskCount") : 0;
        int totalCustomers = churnResult.containsKey("totalCustomers") ? (int) churnResult.get("totalCustomers") : 1;
        double churnRate = churnResult.containsKey("churnRate") ? ((Number) churnResult.get("churnRate")).doubleValue() : 0;
        Number acc = (Number) churnResult.get("modelAccuracy");
        Number auc = (Number) churnResult.get("rocAuc");

        double highRiskPct = totalCustomers > 0 ? (double) highRiskCount / totalCustomers * 100 : 0;
        String severity = highRiskPct > 30 ? "danger" : highRiskPct > 15 ? "warning" : "success";
        String msg = String.format(
                "%d customers (%.0f%% of base) have >70%% churn probability. Historical churn rate: %.1f%%. %s",
                highRiskCount, highRiskPct, churnRate,
                highRiskPct > 15 ? "Proactive outreach or promotions are recommended." : "Churn risk is within acceptable range.");
        insights.add(createInsight("Churn Risk Alert", msg, severity, "churn"));

        if (acc != null) {
            String perfMsg = String.format(
                    "XGBoost churn model trained on RFM features. Accuracy: %.1f%%%s.",
                    acc.doubleValue() * 100,
                    auc != null ? String.format(", ROC-AUC: %.3f", auc.doubleValue()) : "");
            insights.add(createInsight("Churn Model Performance", perfMsg, "info", "churn"));
        }

        // ── SHAP / XAI Explainability ──
        analyzeChurnXAI(churnResult, insights);
    }

    /**
     * Interpret SHAP (SHapley Additive exPlanations) values from the churn model
     * into human-readable insights — Explainable AI (XAI).
     */
    @SuppressWarnings("unchecked")
    private void analyzeChurnXAI(Map<String, Object> churnResult, List<Map<String, Object>> insights) {
        Map<String, Object> globalImportance = (Map<String, Object>) churnResult.get("globalFeatureImportance");
        if (globalImportance == null || globalImportance.isEmpty()) return;

        // Find the most influential feature globally
        String topFeature = null;
        double topValue = -1;
        for (Map.Entry<String, Object> entry : globalImportance.entrySet()) {
            double val = ((Number) entry.getValue()).doubleValue();
            if (val > topValue) {
                topValue = val;
                topFeature = entry.getKey();
            }
        }

        // Build a ranked list
        List<Map.Entry<String, Object>> ranked = new ArrayList<>(globalImportance.entrySet());
        ranked.sort((a, b) -> Double.compare(
                ((Number) b.getValue()).doubleValue(),
                ((Number) a.getValue()).doubleValue()));

        StringBuilder featureRanking = new StringBuilder();
        for (int i = 0; i < ranked.size(); i++) {
            Map.Entry<String, Object> e = ranked.get(i);
            featureRanking.append(String.format("%d) %s (impact: %.4f)", i + 1, e.getKey(), ((Number) e.getValue()).doubleValue()));
            if (i < ranked.size() - 1) featureRanking.append(", ");
        }

        String xaiMsg = String.format(
                "SHAP analysis identifies '%s' as the #1 driver of churn predictions. " +
                "Global feature importance ranking: %s. " +
                "This means %s is the most significant factor the AI model uses to distinguish churners from loyal customers.",
                topFeature, featureRanking, topFeature);
        insights.add(createInsight("🔍 Explainable AI — Churn Drivers", xaiMsg, "info", "churn-xai"));

        // Generate actionable advice based on top feature
        String advice;
        if ("Recency".equals(topFeature)) {
            advice = "Since 'Recency' (days since last transaction) is the strongest churn signal, consider implementing automated re-engagement campaigns (e.g., targeted discounts or reminders) for customers who haven't transacted in 30+ days.";
        } else if ("Frequency".equals(topFeature)) {
            advice = "Since 'Frequency' (number of transactions) is the strongest churn signal, consider introducing loyalty programs or transaction-based rewards to incentivize repeat purchases.";
        } else {
            advice = "Since 'Monetary' (total spending) is the strongest churn signal, high-value customers who reduce spending may be at risk. Consider VIP retention programs or personalized offers for big spenders showing declining activity.";
        }
        insights.add(createInsight("💡 AI Recommendation", advice, "success", "churn-xai"));
    }

    /**
     * Interpret Python sidecar Prophet forecast results into insights.
     */
    public void analyzeCashFlowForecast(Map<String, Object> forecastResult, List<Map<String, Object>> insights) {
        if (forecastResult == null || forecastResult.containsKey("error")) {
            insights.add(createInsight("Cash Flow Forecast", "Cash flow forecasting unavailable — ensure the AI sidecar is running.", "warning", "forecasting"));
            return;
        }

        Number total = (Number) forecastResult.get("totalPredicted");
        Number changePct = (Number) forecastResult.get("changePercent");
        int horizonDays = forecastResult.containsKey("horizonDays") ? (int) forecastResult.get("horizonDays") : 30;

        if (total == null) return;

        String changeStr = changePct != null
                ? String.format(" (%s%.1f%% vs previous %d-day window)", changePct.doubleValue() >= 0 ? "+" : "", changePct.doubleValue(), horizonDays)
                : "";
        String severity = changePct != null && changePct.doubleValue() > 10 ? "success"
                : changePct != null && changePct.doubleValue() < -10 ? "warning" : "info";

        String msg = String.format(
                "Prophet AI predicts MYR %s total revenue over the next %d days%s.",
                String.format("%.2f", total.doubleValue()), horizonDays, changeStr);
        insights.add(createInsight("Cash Flow Forecast 📈", msg, severity, "forecasting"));
    }
}
