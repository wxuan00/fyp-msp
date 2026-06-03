package com.msp.backend.modules.merchant;

import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.util.AuditHelper;
import com.msp.backend.util.MerchantResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantUserMappingRepository merchantUserMappingRepository;
    private final MerchantResolver merchantResolver;

    @GetMapping
    public List<Map<String, Object>> getAllMerchants() {
        User currentUser = getCurrentUser();
        List<Merchant> merchants;
        if ("ADMIN".equals(currentUser.getRole())) {
            merchants = merchantService.getAllMerchants();
        } else {
            List<Long> myMerchantIds = merchantResolver.resolveAllForUser(currentUser);
            if (myMerchantIds.isEmpty()) return List.of();
            merchants = merchantRepository.findAllById(myMerchantIds);
        }
        return merchants.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("merchantId", m.getMerchantId());
            map.put("merchantName", m.getMerchantName());
            map.put("contact", m.getContact());
            map.put("city", m.getCity());
            map.put("country", m.getCountry());
            map.put("status", m.getStatus());
            map.put("addressLine1", m.getAddressLine1());
            map.put("addressLine2", m.getAddressLine2());
            map.put("postcode", m.getPostcode());
            // Linked users
            List<MerchantUserMapping> mappings = merchantUserMappingRepository.findByMerchantId(m.getMerchantId());
            List<Map<String, Object>> linkedUsers = mappings.stream().map(mum -> {
                Map<String, Object> u = new HashMap<>();
                u.put("userId", mum.getUserId());
                userRepository.findById(mum.getUserId()).ifPresent(user -> {
                    u.put("displayName", user.getDisplayName());
                    u.put("email", user.getEmail());
                    u.put("firstName", user.getFirstName());
                    u.put("lastName", user.getLastName());
                });
                return u;
            }).collect(Collectors.toList());
            map.put("linkedUsers", linkedUsers);
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Merchant> getMerchantById(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Merchant merchant = merchantService.getMerchantById(id);

        if (!"ADMIN".equals(currentUser.getRole())) {
            List<Long> myMerchantIds = merchantResolver.resolveAllForUser(currentUser);
            if (!myMerchantIds.contains(merchant.getMerchantId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(merchant);
    }

    @PostMapping
    public ResponseEntity<Merchant> createMerchant(@Valid @RequestBody Merchant merchant) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }
        Merchant created = merchantService.createMerchant(merchant);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/search")
    public List<Merchant> searchMerchants(@RequestParam String name) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) {
            List<Long> myMerchantIds = merchantResolver.resolveAllForUser(currentUser);
            if (myMerchantIds.isEmpty()) return List.of();
            return merchantService.searchMerchants(name).stream()
                    .filter(m -> myMerchantIds.contains(m.getMerchantId()))
                    .toList();
        }
        return merchantService.searchMerchants(name);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Merchant> updateMerchant(@PathVariable Long id, @Valid @RequestBody Merchant merchant) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }
        Merchant updated = merchantService.updateMerchant(id, merchant);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMerchant(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }
        merchantService.deleteMerchant(id);
        return ResponseEntity.noContent().build();
    }

    // ── Merchant ↔ User Mappings (admin only) ───────────────────────────────

    @GetMapping("/{id}/users")
    public ResponseEntity<?> getMerchantUsers(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        userService.populateRole(currentUser);
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());

        // Non-admin: must be linked to this merchant
        if (!isAdmin) {
            boolean isLinked = merchantUserMappingRepository.findByMerchantId(id).stream()
                    .anyMatch(m -> m.getUserId().equals(currentUser.getUserId()));
            if (!isLinked) return ResponseEntity.status(403).build();
        }

        String currentEmail = currentUser.getEmail();
        List<MerchantUserMapping> mappings = merchantUserMappingRepository.findByMerchantId(id);
        List<Map<String, Object>> result = mappings.stream()
            .filter(m -> {
                if (isAdmin) return true;
                // Don't show the current user themselves (they're already viewing)
                if (m.getUserId().equals(currentUser.getUserId())) return true;
                // Child user should not see parent: only show users created by current user,
                // or users created by users they created (recursive descendants), or self
                return isDescendantOf(m.getUserId(), currentEmail);
            })
            .map(m -> {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("userId", m.getUserId());
                row.put("createdAt", m.getCreatedAt());
                row.put("createdBy", m.getCreatedBy());
                row.put("lastModifiedAt", m.getLastModifiedAt());
                row.put("lastModifiedBy", m.getLastModifiedBy());
                userRepository.findById(m.getUserId()).ifPresent(u -> {
                    row.put("email", u.getEmail());
                    row.put("firstName", u.getFirstName());
                    row.put("lastName", u.getLastName());
                    row.put("displayName", u.getDisplayName());
                    row.put("status", u.getStatus());
                });
                return row;
            }).toList();
        return ResponseEntity.ok(result);
    }

    /** Check if userId is a descendant (child, grandchild, etc.) of ancestorEmail */
    private boolean isDescendantOf(Long userId, String ancestorEmail) {
        java.util.Set<String> visited = new java.util.HashSet<>();
        return isDescendantRecursive(userId, ancestorEmail, visited);
    }

    private boolean isDescendantRecursive(Long userId, String ancestorEmail, java.util.Set<String> visited) {
        return userRepository.findById(userId).map(u -> {
            if (ancestorEmail.equals(u.getCreatedBy())) return true;
            if (u.getCreatedBy() == null || u.getCreatedBy().isBlank()) return false;
            if (visited.contains(u.getCreatedBy())) return false;
            visited.add(u.getCreatedBy());
            // Walk up: check if the creator was created by ancestorEmail
            return userRepository.findByEmailAndDeletedAtIsNull(u.getCreatedBy())
                    .map(parent -> isDescendantRecursive(parent.getUserId(), ancestorEmail, visited))
                    .orElse(false);
        }).orElse(false);
    }

    @PostMapping("/{id}/users/{userId}")
    @Transactional
    public ResponseEntity<?> assignUserToMerchant(@PathVariable Long id, @PathVariable Long userId) {
        User currentUser = getCurrentUser();
        userService.populateRole(currentUser);
        boolean isAdmin = "ADMIN".equals(currentUser.getRole());

        if (!isAdmin) {
            // Non-admin: must be linked to this merchant themselves
            boolean isLinked = merchantUserMappingRepository.findByMerchantId(id).stream()
                    .anyMatch(m -> m.getUserId().equals(currentUser.getUserId()));
            if (!isLinked) return ResponseEntity.status(403).build();
        }

        if (!merchantRepository.existsById(id))
            return ResponseEntity.notFound().build();
        if (!userRepository.existsById(userId))
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        if (merchantUserMappingRepository.existsByMerchantIdAndUserId(id, userId))
            return ResponseEntity.badRequest().body(Map.of("error", "User already mapped to this merchant"));

        MerchantUserMapping mapping = new MerchantUserMapping();
        mapping.setMerchantId(id);
        mapping.setUserId(userId);
        String actor = AuditHelper.currentUser();
        mapping.setCreatedBy(actor);
        mapping.setLastModifiedBy(actor);
        merchantUserMappingRepository.save(mapping);
        return ResponseEntity.ok(Map.of("message", "User assigned successfully"));
    }

    @DeleteMapping("/{id}/users/{userId}")
    @Transactional
    public ResponseEntity<?> removeUserFromMerchant(@PathVariable Long id, @PathVariable Long userId) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) return ResponseEntity.status(403).build();

        if (!merchantUserMappingRepository.existsByMerchantIdAndUserId(id, userId))
            return ResponseEntity.notFound().build();
        merchantUserMappingRepository.deleteByMerchantIdAndUserId(id, userId);
        return ResponseEntity.ok(Map.of("message", "User removed successfully"));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
        userService.populateRole(user);
        return user;
    }
}
