package com.msp.backend.modules.merchant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public List<Merchant> getAllMerchants() {
        return merchantRepository.findAll();
    }

    public Merchant getMerchantById(Long id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));
    }

    public Merchant createMerchant(Merchant merchant) {
        return merchantRepository.save(merchant);
    }

    public Merchant updateMerchant(Long id, Merchant updated) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        if (updated.getMerchantName() != null) merchant.setMerchantName(updated.getMerchantName());
        if (updated.getContact() != null) merchant.setContact(updated.getContact());
        if (updated.getAddressLine1() != null) merchant.setAddressLine1(updated.getAddressLine1());
        if (updated.getAddressLine2() != null) merchant.setAddressLine2(updated.getAddressLine2());
        if (updated.getPostcode() != null) merchant.setPostcode(updated.getPostcode());
        if (updated.getCity() != null) merchant.setCity(updated.getCity());
        if (updated.getCountry() != null) merchant.setCountry(updated.getCountry());
        if (updated.getStatus() != null) merchant.setStatus(updated.getStatus());

        return merchantRepository.save(merchant);
    }

    public void deleteMerchant(Long id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));
        merchantRepository.delete(merchant);
    }

    public List<Merchant> searchMerchants(String keyword) {
        return merchantRepository.findByMerchantNameContainingIgnoreCase(keyword);
    }
}
