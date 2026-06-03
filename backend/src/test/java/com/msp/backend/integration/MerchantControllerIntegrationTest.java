package com.msp.backend.integration;

import com.msp.backend.modules.merchant.*;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.util.MerchantResolver;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantController — Integration Tests")
class MerchantControllerIntegrationTest {

    @Mock private MerchantService merchantService;
    @Mock private MerchantRepository merchantRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private MerchantUserMappingRepository merchantUserMappingRepository;
    @Mock private MerchantResolver merchantResolver;

    @InjectMocks private MerchantController merchantController;

    private void mockAuth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Nested @DisplayName("GET /api/merchants")
    class GetAllTests {

        @Test @DisplayName("returns merchant list for admin")
        void returnsList() {
            mockAuth("admin@msp.com");
            User admin = new User(); admin.setEmail("admin@msp.com"); admin.setRole("ADMIN");
            when(userRepository.findByEmailAndDeletedAtIsNull("admin@msp.com")).thenReturn(Optional.of(admin));
            when(merchantService.getAllMerchants()).thenReturn(List.of(new Merchant()));
            List<Map<String, Object>> result = merchantController.getAllMerchants();
            assertThat(result).isNotNull();
        }
    }

    @Nested @DisplayName("GET /api/merchants/{id}")
    class GetByIdTests {

        @Test @DisplayName("returns merchant when found")
        void returnsMerchant() {
            mockAuth("admin@msp.com");
            User admin = new User(); admin.setEmail("admin@msp.com"); admin.setRole("ADMIN");
            when(userRepository.findByEmailAndDeletedAtIsNull("admin@msp.com")).thenReturn(Optional.of(admin));
            Merchant m = new Merchant(); m.setMerchantId(1L); m.setMerchantName("Test");
            when(merchantService.getMerchantById(1L)).thenReturn(m);
            ResponseEntity<Merchant> resp = merchantController.getMerchantById(1L);
            assertThat(resp.getBody().getMerchantName()).isEqualTo("Test");
        }

        @Test @DisplayName("throws when not found")
        void throwsNotFound() {
            mockAuth("admin@msp.com");
            User admin = new User(); admin.setEmail("admin@msp.com"); admin.setRole("ADMIN");
            when(userRepository.findByEmailAndDeletedAtIsNull("admin@msp.com")).thenReturn(Optional.of(admin));
            when(merchantService.getMerchantById(99L)).thenThrow(new RuntimeException("not found"));
            assertThatThrownBy(() -> merchantController.getMerchantById(99L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested @DisplayName("POST /api/merchants")
    class CreateTests {

        @Test @DisplayName("creates merchant successfully")
        void creates() {
            mockAuth("admin@msp.com");
            User admin = new User(); admin.setEmail("admin@msp.com"); admin.setRole("ADMIN");
            when(userRepository.findByEmailAndDeletedAtIsNull("admin@msp.com")).thenReturn(Optional.of(admin));
            Merchant m = new Merchant(); m.setMerchantName("New Merchant");
            when(merchantService.createMerchant(any())).thenReturn(m);
            ResponseEntity<Merchant> resp = merchantController.createMerchant(m);
            assertThat(resp.getBody().getMerchantName()).isEqualTo("New Merchant");
        }
    }

    @Nested @DisplayName("DELETE /api/merchants/{id}")
    class DeleteTests {

        @Test @DisplayName("deletes merchant")
        void deletes() {
            mockAuth("admin@msp.com");
            User admin = new User(); admin.setEmail("admin@msp.com"); admin.setRole("ADMIN");
            when(userRepository.findByEmailAndDeletedAtIsNull("admin@msp.com")).thenReturn(Optional.of(admin));
            doNothing().when(merchantService).deleteMerchant(1L);
            ResponseEntity<Void> resp = merchantController.deleteMerchant(1L);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(merchantService).deleteMerchant(1L);
        }
    }

    @Nested @DisplayName("GET /api/merchants/search")
    class SearchTests {

        @Test @DisplayName("searches by name")
        void searchByName() {
            mockAuth("admin@msp.com");
            User admin = new User(); admin.setEmail("admin@msp.com"); admin.setRole("ADMIN");
            when(userRepository.findByEmailAndDeletedAtIsNull("admin@msp.com")).thenReturn(Optional.of(admin));
            when(merchantService.searchMerchants("Test")).thenReturn(List.of(new Merchant()));
            List<Merchant> result = merchantController.searchMerchants("Test");
            assertThat(result).hasSize(1);
        }
    }
}
