package com.msp.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DataSeeder dataSeeder;

    @PostMapping("/reseed")
    public ResponseEntity<Map<String, String>> reseedDatabase() {
        try {
            dataSeeder.reseed();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Database reseeded successfully",
                "admin_login", "admin@msp.com / admin123",
                "merchant_login", "john.tech@example.com / merchant123"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
