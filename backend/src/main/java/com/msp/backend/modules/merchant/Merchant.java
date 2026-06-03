package com.msp.backend.modules.merchant;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "merchant_name", nullable = false)
    @NotBlank(message = "Merchant name is required")
    @Size(min = 2, max = 200, message = "Merchant name must be between 2 and 200 characters")
    private String merchantName;

    private String contact;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    private String postcode;

    private String city;

    private String country;

    private String status; // ACTIVE, SUSPENDED, PENDING

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}