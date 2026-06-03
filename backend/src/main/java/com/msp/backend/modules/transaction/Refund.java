package com.msp.backend.modules.transaction;

import com.msp.backend.modules.merchant.Merchant;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", insertable = false, updatable = false)
    private Transaction transaction;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", insertable = false, updatable = false)
    private Merchant merchant;

    @Column(name = "card_no")
    private String cardNo;

    @Column(name = "submission_date")
    private LocalDateTime submissionDate;

    @Column(name = "posted_date")
    private LocalDateTime postedDate;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "refund_type")
    private String refundType;

    @Column(name = "refund_ref_no")
    private String refundRefNo;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    private String status;

    @com.fasterxml.jackson.annotation.JsonProperty("merchantName")
    public String getMerchantName() {
        return merchant != null ? merchant.getMerchantName() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.submissionDate == null) this.submissionDate = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
        if (this.currency == null) this.currency = "MYR";
    }
}
