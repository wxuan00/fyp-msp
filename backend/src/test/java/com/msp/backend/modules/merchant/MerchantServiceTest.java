package com.msp.backend.modules.merchant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantService Unit Tests")
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private MerchantService merchantService;

    private Merchant sampleMerchant;
    private Merchant sampleMerchant2;

    @BeforeEach
    void setUp() {
        sampleMerchant = new Merchant();
        sampleMerchant.setMerchantId(1L);
        sampleMerchant.setMerchantName("Tech Store Sdn Bhd");
        sampleMerchant.setContact("+60123456789");
        sampleMerchant.setAddressLine1("No. 1 Jalan Bukit Bintang");
        sampleMerchant.setCity("Kuala Lumpur");
        sampleMerchant.setPostcode("55100");
        sampleMerchant.setCountry("Malaysia");
        sampleMerchant.setStatus("ACTIVE");

        sampleMerchant2 = new Merchant();
        sampleMerchant2.setMerchantId(2L);
        sampleMerchant2.setMerchantName("Food Corner");
        sampleMerchant2.setStatus("PENDING");
    }

    // ─── getAllMerchants ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllMerchants: returns all merchants from repository")
    void getAllMerchants_returnsList() {
        when(merchantRepository.findAll()).thenReturn(Arrays.asList(sampleMerchant, sampleMerchant2));

        List<Merchant> result = merchantService.getAllMerchants();

        assertThat(result).hasSize(2);
        verify(merchantRepository).findAll();
    }

    @Test
    @DisplayName("getAllMerchants: returns empty list when no merchants")
    void getAllMerchants_emptyList() {
        when(merchantRepository.findAll()).thenReturn(List.of());

        List<Merchant> result = merchantService.getAllMerchants();

        assertThat(result).isEmpty();
    }

    // ─── getMerchantById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMerchantById: returns merchant when found")
    void getMerchantById_found_returnsMerchant() {
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(sampleMerchant));

        Merchant result = merchantService.getMerchantById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getMerchantId()).isEqualTo(1L);
        assertThat(result.getMerchantName()).isEqualTo("Tech Store Sdn Bhd");
    }

    @Test
    @DisplayName("getMerchantById: throws RuntimeException when not found")
    void getMerchantById_notFound_throwsException() {
        when(merchantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.getMerchantById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Merchant not found");
    }

    // ─── createMerchant ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createMerchant: saves and returns the merchant")
    void createMerchant_savesAndReturns() {
        when(merchantRepository.save(sampleMerchant)).thenReturn(sampleMerchant);

        Merchant result = merchantService.createMerchant(sampleMerchant);

        assertThat(result).isNotNull();
        assertThat(result.getMerchantName()).isEqualTo("Tech Store Sdn Bhd");
        verify(merchantRepository).save(sampleMerchant);
    }

    // ─── updateMerchant ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateMerchant: updates all provided fields")
    void updateMerchant_updatesAllFields() {
        Merchant updates = new Merchant();
        updates.setMerchantName("New Name");
        updates.setContact("+60199999999");
        updates.setAddressLine1("No. 2 Jalan Ampang");
        updates.setAddressLine2("Level 5");
        updates.setPostcode("50450");
        updates.setCity("Kuala Lumpur");
        updates.setCountry("Malaysia");
        updates.setStatus("SUSPENDED");

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(sampleMerchant));
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        Merchant result = merchantService.updateMerchant(1L, updates);

        assertThat(result.getMerchantName()).isEqualTo("New Name");
        assertThat(result.getContact()).isEqualTo("+60199999999");
        assertThat(result.getStatus()).isEqualTo("SUSPENDED");
        assertThat(result.getCity()).isEqualTo("Kuala Lumpur");
    }

    @Test
    @DisplayName("updateMerchant: partial update - null fields are ignored")
    void updateMerchant_partialUpdate_ignoredNulls() {
        Merchant updates = new Merchant();
        updates.setMerchantName("Partial Update Name");
        // contact, city etc. remain null

        when(merchantRepository.findById(1L)).thenReturn(Optional.of(sampleMerchant));
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        Merchant result = merchantService.updateMerchant(1L, updates);

        assertThat(result.getMerchantName()).isEqualTo("Partial Update Name");
        assertThat(result.getContact()).isEqualTo("+60123456789"); // unchanged
        assertThat(result.getCity()).isEqualTo("Kuala Lumpur"); // unchanged
    }

    @Test
    @DisplayName("updateMerchant: throws when merchant not found")
    void updateMerchant_notFound_throwsException() {
        when(merchantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.updateMerchant(99L, new Merchant()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Merchant not found");
    }

    // ─── deleteMerchant ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteMerchant: deletes merchant successfully")
    void deleteMerchant_deletesSuccessfully() {
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(sampleMerchant));
        doNothing().when(merchantRepository).delete(sampleMerchant);

        assertThatCode(() -> merchantService.deleteMerchant(1L)).doesNotThrowAnyException();

        verify(merchantRepository).delete(sampleMerchant);
    }

    @Test
    @DisplayName("deleteMerchant: throws when merchant not found")
    void deleteMerchant_notFound_throwsException() {
        when(merchantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.deleteMerchant(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Merchant not found");

        verify(merchantRepository, never()).delete(any());
    }

    // ─── searchMerchants ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchMerchants: returns merchants matching keyword")
    void searchMerchants_returnsMatching() {
        when(merchantRepository.findByMerchantNameContainingIgnoreCase("tech"))
                .thenReturn(List.of(sampleMerchant));

        List<Merchant> result = merchantService.searchMerchants("tech");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMerchantName()).containsIgnoringCase("tech");
    }

    @Test
    @DisplayName("searchMerchants: case-insensitive matching")
    void searchMerchants_caseInsensitive() {
        when(merchantRepository.findByMerchantNameContainingIgnoreCase("FOOD"))
                .thenReturn(List.of(sampleMerchant2));

        List<Merchant> result = merchantService.searchMerchants("FOOD");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMerchantName()).isEqualTo("Food Corner");
    }

    @Test
    @DisplayName("searchMerchants: returns empty when no match")
    void searchMerchants_noMatch_returnsEmpty() {
        when(merchantRepository.findByMerchantNameContainingIgnoreCase("xyz"))
                .thenReturn(List.of());

        List<Merchant> result = merchantService.searchMerchants("xyz");

        assertThat(result).isEmpty();
    }
}
