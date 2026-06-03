package com.msp.backend.modules.settlement;

import com.msp.backend.modules.merchant.Merchant;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_advices")
public class CreditAdvice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credit_advice_id")
    private Long creditAdviceId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", insertable = false, updatable = false)
    private Merchant merchant;

    @Column(name = "account_no")
    private String accountNo;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "account_id")
    private String accountId;

    @com.fasterxml.jackson.annotation.JsonProperty("merchantName")
    public String getMerchantName() {
        return merchant != null ? merchant.getMerchantName() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.paymentDate == null) this.paymentDate = LocalDateTime.now();
        if (this.currency == null) this.currency = "MYR";
    }
}
