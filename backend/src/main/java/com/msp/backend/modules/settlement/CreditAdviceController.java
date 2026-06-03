package com.msp.backend.modules.settlement;

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
@RequestMapping("/api/credit-advices")
@RequiredArgsConstructor
public class CreditAdviceController {

    private final CreditAdviceService creditAdviceService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantResolver merchantResolver;

    @GetMapping
    public Page<CreditAdvice> getAllCreditAdvices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        User currentUser = getCurrentUser();
        Long restrictToMerchantId = null;
        if (!"ADMIN".equals(currentUser.getRole())) {
            restrictToMerchantId = getMyMerchantId(currentUser);
            if (restrictToMerchantId == null) return Page.empty();
        }
        return creditAdviceService.getCreditAdvicesPage(
                restrictToMerchantId, merchantName, accountId, dateFrom, dateTo,
                page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditAdvice> getCreditAdviceById(@PathVariable Long id) {
        CreditAdvice advice = creditAdviceService.getCreditAdviceById(id);
        User currentUser = getCurrentUser();

        if (!"ADMIN".equals(currentUser.getRole())) {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null || !merchantId.equals(advice.getMerchantId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(advice);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
        userService.populateRole(user);
        return user;
    }

    private Long getMyMerchantId(User user) {
        return merchantResolver.resolveForUser(user);
    }
}
