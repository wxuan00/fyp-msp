package com.msp.backend.modules.transaction;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
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

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantRepository merchantRepository;
    private final MerchantResolver merchantResolver;

    @GetMapping
    public Page<Transaction> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "txnDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) String txnId,
            @RequestParam(required = false) String cardNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        User currentUser = getCurrentUser();
        java.util.List<Long> restrictToMerchantIds = null;
        if (!"ADMIN".equals(currentUser.getRole())) {
            restrictToMerchantIds = merchantResolver.resolveAllForUser(currentUser);
            if (restrictToMerchantIds.isEmpty()) {
                return org.springframework.data.domain.Page.empty();
            }
        }
        return transactionService.getTransactionsPage(
                restrictToMerchantIds, merchantName, txnId, cardNo, status, channel, dateFrom, dateTo,
                page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        Transaction txn = transactionService.getTransactionById(id);
        User currentUser = getCurrentUser();

        if (!"ADMIN".equals(currentUser.getRole())) {
            java.util.List<Long> myMerchantIds = merchantResolver.resolveAllForUser(currentUser);
            if (myMerchantIds.isEmpty() || !myMerchantIds.contains(txn.getMerchantId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(txn);
    }

    @GetMapping("/by-settlement/{settlementId}")
    public ResponseEntity<?> getTransactionsBySettlement(@PathVariable Long settlementId) {
        return ResponseEntity.ok(transactionRepository.findBySettlementId(settlementId));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
        userService.populateRole(user);
        return user;
    }
}
