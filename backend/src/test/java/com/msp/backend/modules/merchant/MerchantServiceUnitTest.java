package com.msp.backend.modules.merchant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for MerchantService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantService — Unit Tests")
class MerchantServiceUnitTest {

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private MerchantService merchantService;

    private Merchant sampleMerchant;

    @BeforeEach
    void setUp() {
        sampleMerchant = new Merchant();
        sampleMerchant.setMerchantId(1L);
        sampleMerchant.setMerchantName("Test Merchant");
        sampleMerchant.setContact("012-3456789");
        sampleMerchant.setStatus("ACTIVE");
        sampleMerchant.setCity("KL");
        sampleMerchant.setCountry("MY");
    }

    @Nested
    @DisplayName("getAllMerchants")
    class GetAllMerchantsTests {

        @Test
        @DisplayName("returns all merchants from repository")
        void returnsAll() {
            when(merchantRepository.findAll()).thenReturn(List.of(sampleMerchant));
            List<Merchant> result = merchantService.getAllMerchants();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMerchantName()).isEqualTo("Test Merchant");
        }

        @Test
        @DisplayName("returns empty list when no merchants exist")
        void returnsEmpty() {
            when(merchantRepository.findAll()).thenReturn(List.of());
            assertThat(merchantService.getAllMerchants()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMerchantById")
    class GetMerchantByIdTests {

        @Test
        @DisplayName("returns merchant when found")
        void returnsMerchant() {
            when(merchantRepository.findById(1L)).thenReturn(Optional.of(sampleMerchant));
            Merchant result = merchantService.getMerchantById(1L);
            assertThat(result.getMerchantName()).isEqualTo("Test Merchant");
        }

        @Test
        @DisplayName("throws when merchant not found")
        void throwsWhenNotFound() {
            when(merchantRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> merchantService.getMerchantById(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("createMerchant")
    class CreateMerchantTests {

        @Test
        @DisplayName("saves and returns new merchant")
        void savesAndReturns() {
            when(merchantRepository.save(any(Merchant.class))).thenReturn(sampleMerchant);
            Merchant result = merchantService.createMerchant(sampleMerchant);
            assertThat(result.getMerchantId()).isEqualTo(1L);
            verify(merchantRepository).save(sampleMerchant);
        }
    }

    @Nested
    @DisplayName("updateMerchant")
    class UpdateMerchantTests {

        @Test
        @DisplayName("updates only non-null fields")
        void partialUpdate() {
            when(merchantRepository.findById(1L)).thenReturn(Optional.of(sampleMerchant));
            when(merchantRepository.save(any(Merchant.class))).thenAnswer(i -> i.getArgument(0));

            Merchant updates = new Merchant();
            updates.setCity("Penang");

            Merchant result = merchantService.updateMerchant(1L, updates);
            assertThat(result.getCity()).isEqualTo("Penang");
            assertThat(result.getMerchantName()).isEqualTo("Test Merchant"); // unchanged
        }

        @Test
        @DisplayName("throws when merchant not found")
        void throwsWhenNotFound() {
            when(merchantRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> merchantService.updateMerchant(99L, new Merchant()))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("deleteMerchant")
    class DeleteMerchantTests {

        @Test
        @DisplayName("deletes existing merchant")
        void deletesExisting() {
            when(merchantRepository.findById(1L)).thenReturn(Optional.of(sampleMerchant));
            doNothing().when(merchantRepository).delete(sampleMerchant);
            merchantService.deleteMerchant(1L);
            verify(merchantRepository).delete(sampleMerchant);
        }

        @Test
        @DisplayName("throws when merchant not found")
        void throwsWhenNotFound() {
            when(merchantRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> merchantService.deleteMerchant(99L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("searchMerchants")
    class SearchMerchantsTests {

        @Test
        @DisplayName("returns matching merchants by name")
        void searchByName() {
            when(merchantRepository.findByMerchantNameContainingIgnoreCase("Test"))
                    .thenReturn(List.of(sampleMerchant));
            List<Merchant> result = merchantService.searchMerchants("Test");
            assertThat(result).hasSize(1);
        }
    }
}
