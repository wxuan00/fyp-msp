package com.msp.backend.modules.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates user IDs in the format: yyyyMMddHHmmss + 5-digit zero-padded sequence.
 * Example: 2026042012345600001
 * Up to 99,999 unique IDs can be generated per second.
 */
@Component
@RequiredArgsConstructor
public class UserIdGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final UserRepository userRepository;

    public synchronized String generate() {
        String prefix = LocalDateTime.now().format(FMT);

        List<String> existing = userRepository.findUserIdsByPrefix(prefix);
        int maxSeq = existing.stream()
                .filter(id -> id.length() == prefix.length() + 5)
                .mapToInt(id -> Integer.parseInt(id.substring(prefix.length())))
                .max().orElse(0);
        int nextSeq = maxSeq + 1;

        if (nextSeq > 99_999) {
            throw new RuntimeException("User ID sequence exhausted for this second. Try again.");
        }
        return prefix + String.format("%05d", nextSeq);
    }
}
