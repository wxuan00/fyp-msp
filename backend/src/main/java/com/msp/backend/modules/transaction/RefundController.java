package com.msp.backend.modules.transaction;

import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.util.MerchantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantResolver merchantResolver;

    @GetMapping
    public Page<Refund> getAllRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submissionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) String refundRefNo,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String cardNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String refundType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        User currentUser = getCurrentUser();
        java.util.List<Long> restrictToMerchantIds = null;
        if (!"ADMIN".equals(currentUser.getRole())) {
            restrictToMerchantIds = merchantResolver.resolveAllForUser(currentUser);
            if (restrictToMerchantIds.isEmpty()) return Page.empty();
        }
        return refundService.getRefundsPage(
                restrictToMerchantIds, merchantName, refundRefNo, transactionId, cardNo, status, refundType, dateFrom, dateTo,
                page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Refund> getRefundById(@PathVariable Long id) {
        Refund refund = refundService.getRefundById(id);
        User currentUser = getCurrentUser();

        if (!"ADMIN".equals(currentUser.getRole())) {
            java.util.List<Long> myMerchantIds = merchantResolver.resolveAllForUser(currentUser);
            if (myMerchantIds.isEmpty() || !myMerchantIds.contains(refund.getMerchantId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(refund);
    }

    @PostMapping
    public ResponseEntity<?> requestRefund(@RequestBody Refund refund) {
        try {
            Refund saved = refundService.requestRefund(refund);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelRefund(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Refund refund = refundService.getRefundById(id);
            if (!"ADMIN".equals(currentUser.getRole())) {
                java.util.List<Long> myMerchantIds = merchantResolver.resolveAllForUser(currentUser);
                if (myMerchantIds.isEmpty() || !myMerchantIds.contains(refund.getMerchantId())) {
                    return ResponseEntity.status(403).build();
                }
            }
            Refund updated = refundService.cancelRefund(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
        userService.populateRole(user);
        return user;
    }
}
