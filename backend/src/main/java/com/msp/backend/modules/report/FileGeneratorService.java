package com.msp.backend.modules.report;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.msp.backend.modules.settlement.Settlement;
import com.msp.backend.modules.transaction.Transaction;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class FileGeneratorService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── PDF helpers ────────────────────────────────────────────────────────────
    private final Color HEADER_BG  = new Color(30, 64, 175);   // blue-800
    private final Color ALT_ROW_BG = new Color(239, 246, 255); // blue-50
    private final Font  TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD,   Color.WHITE);
    private final Font  HEAD_FONT  = new Font(Font.HELVETICA, 9,  Font.BOLD,   Color.WHITE);
    private final Font  BODY_FONT  = new Font(Font.HELVETICA, 8,  Font.NORMAL, Color.BLACK);
    private final Font  META_FONT  = new Font(Font.HELVETICA, 9,  Font.NORMAL, new Color(75, 85, 99));
    private final Font  KPI_LABEL  = new Font(Font.HELVETICA, 9,  Font.BOLD,   new Color(55, 65, 81));
    private final Font  KPI_VALUE  = new Font(Font.HELVETICA, 11, Font.BOLD,   new Color(30, 64, 175));

    private Document createDocument(ByteArrayOutputStream out, String title, String subtitle) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // Header banner
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        PdfPCell bannerCell = new PdfPCell(new Phrase(title, TITLE_FONT));
        bannerCell.setBackgroundColor(HEADER_BG);
        bannerCell.setPadding(12);
        bannerCell.setBorder(Rectangle.NO_BORDER);
        banner.addCell(bannerCell);
        doc.add(banner);
        doc.add(new Paragraph(" "));

        // Subtitle / meta
        doc.add(new Paragraph(subtitle, META_FONT));
        doc.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), META_FONT));
        doc.add(new Paragraph(" "));
        return doc;
    }

    private PdfPCell headerCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, HEAD_FONT));
        c.setBackgroundColor(HEADER_BG);
        c.setPadding(6);
        c.setBorderColor(Color.WHITE);
        return c;
    }

    private PdfPCell bodyCell(String text, boolean alt) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, BODY_FONT));
        c.setBackgroundColor(alt ? ALT_ROW_BG : Color.WHITE);
        c.setPadding(5);
        c.setBorderColor(new Color(209, 213, 219));
        return c;
    }

    // ── CSV methods (unchanged) ────────────────────────────────────────────────

    public byte[] generateTransactionsCsv(List<Transaction> transactions) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);

        writer.println("Transaction ID,Merchant,Payment Channel,Amount,Nett Amount,Discount,Currency,Status,Ref No,Card No,Txn Date,Posted Date,Description");

        for (Transaction t : transactions) {
            writer.printf("%d,\"%s\",%s,%.2f,%.2f,%.2f,%s,%s,%s,%s,%s,%s,\"%s\"%n",
                    t.getTransactionId(),
                    escapeCsv(t.getMerchantName()),
                    t.getPaymentChannel() != null ? t.getPaymentChannel() : "",
                    t.getAmount() != null ? t.getAmount() : java.math.BigDecimal.ZERO,
                    t.getNettAmount() != null ? t.getNettAmount() : java.math.BigDecimal.ZERO,
                    t.getDiscountAmount() != null ? t.getDiscountAmount() : java.math.BigDecimal.ZERO,
                    t.getCurrency() != null ? t.getCurrency() : "",
                    t.getStatus() != null ? t.getStatus() : "",
                    t.getRefNo() != null ? t.getRefNo() : "",
                    t.getCardNo() != null ? t.getCardNo() : "",
                    t.getTxnDate() != null ? t.getTxnDate().format(DT_FMT) : "",
                    t.getPostedDate() != null ? t.getPostedDate().format(DT_FMT) : "",
                    escapeCsv(t.getTxnDescription())
            );
        }

        writer.flush();
        return out.toByteArray();
    }

    public byte[] generateSettlementsCsv(List<Settlement> settlements) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);

        writer.println("Settlement ID,Settlement No,Settlement Type,Credit Advice ID,Currency,Settlement Amount,Payment Amount,Settlement Date");

        for (Settlement s : settlements) {
            writer.printf("%d,%s,%s,%s,%s,%.2f,%.2f,%s%n",
                    s.getSettlementId(),
                    s.getSettlementNo() != null ? s.getSettlementNo() : "",
                    s.getSettlementType() != null ? s.getSettlementType() : "",
                    s.getCreditAdviceId() != null ? s.getCreditAdviceId().toString() : "",
                    s.getCurrency() != null ? s.getCurrency() : "",
                    s.getSettlementAmount() != null ? s.getSettlementAmount() : java.math.BigDecimal.ZERO,
                    s.getPaymentAmount() != null ? s.getPaymentAmount() : java.math.BigDecimal.ZERO,
                    s.getSettlementDate() != null ? s.getSettlementDate().format(DT_FMT) : ""
            );
        }

        writer.flush();
        return out.toByteArray();
    }

    public byte[] generateSummaryReportCsv(Map<String, Object> reportData) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);

        writer.println("Metric,Value");
        writer.printf("Report Type,%s%n", reportData.getOrDefault("type", ""));
        writer.printf("Generated By,%s%n", reportData.getOrDefault("generatedBy", ""));
        writer.printf("Generated At,%s%n", reportData.getOrDefault("generatedAt", ""));
        writer.println();

        if ("ADMIN_SUMMARY".equals(reportData.get("type"))) {
            writer.printf("Total Users,%s%n", reportData.getOrDefault("totalUsers", "0"));
            writer.printf("Total Merchants,%s%n", reportData.getOrDefault("totalMerchants", "0"));
            writer.printf("Active Merchants,%s%n", reportData.getOrDefault("activeMerchants", "0"));
            writer.printf("Pending Merchants,%s%n", reportData.getOrDefault("pendingMerchants", "0"));
            writer.printf("Suspended Merchants,%s%n", reportData.getOrDefault("suspendedMerchants", "0"));
        } else {
            writer.printf("Merchant Name,%s%n", reportData.getOrDefault("merchantName", ""));
            writer.printf("Merchant Status,%s%n", reportData.getOrDefault("merchantStatus", ""));
        }

        writer.printf("Total Transactions,%s%n", reportData.getOrDefault("totalTransactions", "0"));
        writer.printf("Approved Transactions,%s%n", reportData.getOrDefault("approvedTransactions", "0"));
        writer.printf("Pending Transactions,%s%n", reportData.getOrDefault("pendingTransactions", "0"));
        writer.printf("Declined Transactions,%s%n", reportData.getOrDefault("declinedTransactions", "0"));
        writer.printf("Total Settlements,%s%n", reportData.getOrDefault("totalSettlements", "0"));

        writer.flush();
        return out.toByteArray();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    // ── PDF methods ────────────────────────────────────────────────────────────

    public byte[] generateSummaryReportPdf(Map<String, Object> data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            String type = String.valueOf(data.getOrDefault("type", ""));
            boolean isAdmin = "ADMIN_SUMMARY".equals(type);
            Document doc = createDocument(out,
                    isAdmin ? "MSP Admin Summary Report" : "MSP Merchant Summary Report",
                    "Generated by: " + data.getOrDefault("generatedBy", ""));

            // KPI table
            int cols = isAdmin ? 4 : 3;
            PdfPTable kpi = new PdfPTable(cols);
            kpi.setWidthPercentage(100);
            kpi.setSpacingBefore(4);

            java.util.function.BiConsumer<String, String> addKpi = (label, value) -> {
                PdfPCell cell = new PdfPCell();
                cell.addElement(new Phrase(label, KPI_LABEL));
                cell.addElement(new Phrase(value, KPI_VALUE));
                cell.setPadding(10);
                cell.setBackgroundColor(new Color(239, 246, 255));
                cell.setBorderColor(new Color(30, 64, 175));
                kpi.addCell(cell);
            };

            addKpi.accept("Total Transactions", String.valueOf(data.getOrDefault("totalTransactions", 0)));
            addKpi.accept("Approved",           String.valueOf(data.getOrDefault("approvedTransactions", 0)));
            addKpi.accept("Total Settlements",  String.valueOf(data.getOrDefault("totalSettlements", 0)));
            if (isAdmin) {
                addKpi.accept("Total Merchants", String.valueOf(data.getOrDefault("totalMerchants", 0)));
            }
            doc.add(kpi);
            doc.add(new Paragraph(" "));

            // Details table
            PdfPTable tbl = new PdfPTable(2);
            tbl.setWidthPercentage(60);
            tbl.setHorizontalAlignment(Element.ALIGN_LEFT);
            tbl.addCell(headerCell("Metric"));
            tbl.addCell(headerCell("Value"));
            int row = 0;
            for (Map.Entry<String, Object> e : data.entrySet()) {
                if (e.getKey().equals("type") || e.getKey().equals("generatedBy") || e.getKey().equals("generatedAt")) continue;
                tbl.addCell(bodyCell(e.getKey(), row % 2 == 0));
                tbl.addCell(bodyCell(String.valueOf(e.getValue()), row % 2 == 0));
                row++;
            }
            doc.add(tbl);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
        return out.toByteArray();
    }

    public byte[] generateTransactionsPdf(List<Transaction> transactions) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document doc = createDocument(out, "MSP Transaction Report",
                    transactions.size() + " transaction(s)");
            PdfPTable tbl = new PdfPTable(new float[]{1.2f, 2f, 1.5f, 1.5f, 1.5f, 1.2f, 1.2f, 2f});
            tbl.setWidthPercentage(100);
            for (String h : new String[]{"ID", "Merchant", "Amount", "Nett Amt", "Channel", "Currency", "Status", "Txn Date"})
                tbl.addCell(headerCell(h));
            int row = 0;
            for (Transaction t : transactions) {
                boolean alt = row++ % 2 == 1;
                tbl.addCell(bodyCell(String.valueOf(t.getTransactionId()), alt));
                tbl.addCell(bodyCell(t.getMerchantName(), alt));
                tbl.addCell(bodyCell(t.getAmount() != null ? t.getAmount().toPlainString() : "", alt));
                tbl.addCell(bodyCell(t.getNettAmount() != null ? t.getNettAmount().toPlainString() : "", alt));
                tbl.addCell(bodyCell(t.getPaymentChannel(), alt));
                tbl.addCell(bodyCell(t.getCurrency(), alt));
                tbl.addCell(bodyCell(t.getStatus(), alt));
                tbl.addCell(bodyCell(t.getTxnDate() != null ? t.getTxnDate().format(DT_FMT) : "", alt));
            }
            doc.add(tbl);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
        return out.toByteArray();
    }

    public byte[] generateSettlementsPdf(List<Settlement> settlements) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document doc = createDocument(out, "MSP Settlement Report",
                    settlements.size() + " settlement(s)");
            PdfPTable tbl = new PdfPTable(new float[]{1f, 2f, 1.5f, 2f, 1.2f, 1.8f, 1.8f, 2f});
            tbl.setWidthPercentage(100);
            for (String h : new String[]{"ID", "Settlement No", "Type", "Credit Advice ID", "Currency", "Sett. Amount", "Payment Amt", "Date"})
                tbl.addCell(headerCell(h));
            int row = 0;
            for (Settlement s : settlements) {
                boolean alt = row++ % 2 == 1;
                tbl.addCell(bodyCell(String.valueOf(s.getSettlementId()), alt));
                tbl.addCell(bodyCell(s.getSettlementNo(), alt));
                tbl.addCell(bodyCell(s.getSettlementType(), alt));
                tbl.addCell(bodyCell(s.getCreditAdviceId() != null ? s.getCreditAdviceId().toString() : "", alt));
                tbl.addCell(bodyCell(s.getCurrency(), alt));
                tbl.addCell(bodyCell(s.getSettlementAmount() != null ? s.getSettlementAmount().toPlainString() : "", alt));
                tbl.addCell(bodyCell(s.getPaymentAmount() != null ? s.getPaymentAmount().toPlainString() : "", alt));
                tbl.addCell(bodyCell(s.getSettlementDate() != null ? s.getSettlementDate().format(DT_FMT) : "", alt));
            }
            doc.add(tbl);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
        return out.toByteArray();
    }
}
