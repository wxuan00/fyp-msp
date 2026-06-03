package com.msp.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOriginPatterns(java.util.List.of("*"));
                    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(java.util.List.of("*"));
                    config.setExposedHeaders(java.util.List.of("Authorization", "Content-Disposition"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/auth/mfa/verify").authenticated()
                        // Admin-only endpoints: role management
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/roles/**").authenticated()
                        // Profile endpoints: accessible by all authenticated users
                        .requestMatchers("/api/profile/**").authenticated()
                        // Merchant, Transaction, Settlement, Credit Advice endpoints (filtered by role in controller)
                        .requestMatchers("/api/merchants/**").authenticated()
                        .requestMatchers("/api/transactions/**").authenticated()
                        .requestMatchers("/api/refunds/**").authenticated()
                        .requestMatchers("/api/settlements/**").authenticated()
                        .requestMatchers("/api/credit-advices/**").authenticated()
                        // Dashboard, Analytics & Reports: accessible by all authenticated users
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers("/api/analytics/**").authenticated()
                        .requestMatchers("/api/reports/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}