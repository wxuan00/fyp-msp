package com.msp.backend.modules.report;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.settlement.Settlement;
import com.msp.backend.modules.settlement.SettlementService;
import com.msp.backend.modules.transaction.Transaction;
import com.msp.backend.modules.transaction.TransactionService;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.util.MerchantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantRepository merchantRepository;
    private final TransactionService transactionService;
    private final SettlementService settlementService;
    private final FileGeneratorService fileGeneratorService;
    private final MerchantResolver merchantResolver;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummaryReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        Map<String, Object> report = buildSummaryReport(currentUser, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    // ── Date filter helpers ──────────────────────────────────────
    private List<Transaction> filterByDateRange(List<Transaction> txns, String startDate, String endDate) {
        if (startDate == null && endDate == null) return txns;
        LocalDateTime from = startDate != null && !startDate.isBlank()
                ? LocalDate.parse(startDate).atStartOfDay() : LocalDateTime.MIN;
        LocalDateTime to = endDate != null && !endDate.isBlank()
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : LocalDateTime.MAX;
        return txns.stream()
                .filter(t -> t.getTxnDate() != null
                        && !t.getTxnDate().isBefore(from)
                        && !t.getTxnDate().isAfter(to))
                .collect(Collectors.toList());
    }

    private List<Settlement> filterSettlementsByDateRange(List<Settlement> settlements, String startDate, String endDate) {
        if (startDate == null && endDate == null) return settlements;
        LocalDateTime from = startDate != null && !startDate.isBlank()
                ? LocalDate.parse(startDate).atStartOfDay() : LocalDateTime.MIN;
        LocalDateTime to = endDate != null && !endDate.isBlank()
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : LocalDateTime.MAX;
        return settlements.stream()
                .filter(s -> s.getSettlementDate() != null
                        && !s.getSettlementDate().isBefore(from)
                        && !s.getSettlementDate().isAfter(to))
                .collect(Collectors.toList());
    }

    private String buildFilename(String base, String startDate, String endDate, String ext) {
        if (startDate != null && endDate != null) {
            return base + "_" + startDate + "_to_" + endDate + "." + ext;
        } else if (startDate != null) {
            return base + "_from_" + startDate + "." + ext;
        } else if (endDate != null) {
            return base + "_until_" + endDate + "." + ext;
        }
        return base + "." + ext;
    }
    // ────────────────────────────────────────────────────────────

    private Map<String, Object> buildSummaryReport(User currentUser, String startDate, String endDate) {
        Map<String, Object> report = new HashMap<>();
        report.put("generatedBy", currentUser.getEmail());
        report.put("generatedAt", java.time.LocalDateTime.now().toString());
        if (startDate != null && !startDate.isBlank()) report.put("dateFrom", startDate);
        if (endDate != null && !endDate.isBlank()) report.put("dateTo", endDate);

        if ("ADMIN".equals(currentUser.getRole())) {
            report.put("type", "ADMIN_SUMMARY");
            report.put("totalUsers", userRepository.findByDeletedAtIsNull().size());
            report.put("totalMerchants", merchantRepository.count());

            var merchants = merchantRepository.findAll();
            report.put("activeMerchants", merchants.stream().filter(m -> "ACTIVE".equals(m.getStatus())).count());
            report.put("pendingMerchants", merchants.stream().filter(m -> "PENDING".equals(m.getStatus())).count());
            report.put("suspendedMerchants", merchants.stream().filter(m -> "SUSPENDED".equals(m.getStatus())).count());

            var transactions = filterByDateRange(transactionService.getAllTransactions(), startDate, endDate);
            report.put("totalTransactions", transactions.size());
            report.put("approvedTransactions", transactions.stream().filter(t -> "APPROVED".equals(t.getStatus())).count());
            report.put("pendingTransactions", transactions.stream().filter(t -> "PENDING".equals(t.getStatus())).count());
            report.put("declinedTransactions", transactions.stream().filter(t -> "DECLINED".equals(t.getStatus())).count());

            report.put("totalSettlements", filterSettlementsByDateRange(settlementService.getAllSettlements(), startDate, endDate).size());
        } else {
            report.put("type", "MERCHANT_SUMMARY");
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId != null) {
                var merchant = merchantRepository.findById(merchantId);
                report.put("merchantName", merchant.map(Merchant::getMerchantName).orElse("Unknown"));
                report.put("merchantStatus", merchant.map(Merchant::getStatus).orElse("Unknown"));

                var transactions = filterByDateRange(transactionService.getTransactionsByMerchantId(merchantId), startDate, endDate);
                report.put("totalTransactions", transactions.size());
                report.put("approvedTransactions", transactions.stream().filter(t -> "APPROVED".equals(t.getStatus())).count());
                report.put("pendingTransactions", transactions.stream().filter(t -> "PENDING".equals(t.getStatus())).count());
                report.put("declinedTransactions", transactions.stream().filter(t -> "DECLINED".equals(t.getStatus())).count());

                var settlements = filterSettlementsByDateRange(settlementService.getSettlementsByMerchantId(merchantId), startDate, endDate);
                report.put("totalSettlements", settlements.size());
            }
        }

        return report;
    }

    @GetMapping("/summary/export")
    public ResponseEntity<byte[]> exportSummaryReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        Map<String, Object> report = buildSummaryReport(currentUser, startDate, endDate);
        byte[] csv = fileGeneratorService.generateSummaryReportCsv(report);

        String filename = buildFilename("summary-report", startDate, endDate, "csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/transactions/export")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        List<Transaction> transactions;
        if ("ADMIN".equals(currentUser.getRole())) {
            transactions = transactionService.getAllTransactions();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            transactions = merchantId != null
                    ? transactionService.getTransactionsByMerchantId(merchantId)
                    : List.of();
        }
        transactions = filterByDateRange(transactions, startDate, endDate);
        byte[] csv = fileGeneratorService.generateTransactionsCsv(transactions);

        String filename = buildFilename("transactions-export", startDate, endDate, "csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/settlements/export")
    public ResponseEntity<byte[]> exportSettlements(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        List<Settlement> settlements;
        if ("ADMIN".equals(currentUser.getRole())) {
            settlements = settlementService.getAllSettlements();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            settlements = merchantId != null
                    ? settlementService.getSettlementsByMerchantId(merchantId)
                    : List.of();
        }
        settlements = filterSettlementsByDateRange(settlements, startDate, endDate);
        byte[] csv = fileGeneratorService.generateSettlementsCsv(settlements);

        String filename = buildFilename("settlements-export", startDate, endDate, "csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // ── PDF export endpoints ─────────────────────────────────────────────────

    @GetMapping("/summary/export/pdf")
    public ResponseEntity<byte[]> exportSummaryReportPdf(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        Map<String, Object> report = buildSummaryReport(currentUser, startDate, endDate);
        byte[] pdf = fileGeneratorService.generateSummaryReportPdf(report);
        String filename = buildFilename("summary-report", startDate, endDate, "pdf");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/transactions/export/pdf")
    public ResponseEntity<byte[]> exportTransactionsPdf(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        List<Transaction> transactions;
        if ("ADMIN".equals(currentUser.getRole())) {
            transactions = transactionService.getAllTransactions();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            transactions = merchantId != null
                    ? transactionService.getTransactionsByMerchantId(merchantId)
                    : List.of();
        }
        transactions = filterByDateRange(transactions, startDate, endDate);
        byte[] pdf = fileGeneratorService.generateTransactionsPdf(transactions);
        String filename = buildFilename("transactions-export", startDate, endDate, "pdf");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/settlements/export/pdf")
    public ResponseEntity<byte[]> exportSettlementsPdf(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        User currentUser = getCurrentUser();
        List<Settlement> settlements;
        if ("ADMIN".equals(currentUser.getRole())) {
            settlements = settlementService.getAllSettlements();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            settlements = merchantId != null
                    ? settlementService.getSettlementsByMerchantId(merchantId)
                    : List.of();
        }
        settlements = filterSettlementsByDateRange(settlements, startDate, endDate);
        byte[] pdf = fileGeneratorService.generateSettlementsPdf(settlements);
        String filename = buildFilename("settlements-export", startDate, endDate, "pdf");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userService.populateRole(user);
        return user;
    }

    private Long getMyMerchantId(User user) {
        return merchantResolver.resolveForUser(user);
    }
}
