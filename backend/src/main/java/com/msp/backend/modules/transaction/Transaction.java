package com.msp.backend.modules.transaction;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.settlement.Settlement;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", insertable = false, updatable = false)
    private Merchant merchant;

    @Column(name = "settlement_id")
    private Long settlementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", insertable = false, updatable = false)
    private Settlement settlement;

    @Column(name = "payment_channel")
    private String paymentChannel;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "txn_date")
    private LocalDateTime txnDate;

    @Column(name = "ref_no")
    private String refNo;

    @Column(name = "card_no")
    private String cardNo;

    @Column(nullable = false)
    private String currency;

    @Column(name = "posted_date")
    private LocalDateTime postedDate;

    @Column(name = "txn_description")
    private String txnDescription;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "nett_amount", precision = 12, scale = 2)
    private BigDecimal nettAmount;

    @com.fasterxml.jackson.annotation.JsonProperty("merchantName")
    public String getMerchantName() {
        return merchant != null ? merchant.getMerchantName() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.txnDate == null) this.txnDate = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
        if (this.currency == null) this.currency = "MYR";
    }
}
