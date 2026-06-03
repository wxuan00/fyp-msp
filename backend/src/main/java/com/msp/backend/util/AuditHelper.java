package com.msp.backend.util;

import com.msp.backend.modules.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditHelper {

    public static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    /**
     * Resolves an audit email to a human-readable name:
     * displayName if set, otherwise firstName + lastName.
     * Falls back to the original value if the user cannot be found.
     */
    public static String resolveDisplayName(String email, UserRepository userRepository) {
        if (email == null || email.isBlank() || "SYSTEM".equals(email)) return email;
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .map(u -> {
                    if (u.getDisplayName() != null && !u.getDisplayName().isBlank())
                        return u.getDisplayName();
                    return (u.getFirstName() + " " + u.getLastName()).trim();
                })
                .orElse(email);
    }
}
