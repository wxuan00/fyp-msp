package com.msp.backend.modules.auth.dto;
import lombok.Data;

@Data
public class LoginRequest {
    private String identifier; // accepts email or display name
    private String password;

    // Backwards-compatible: if old clients send "email", treat it as identifier
    public void setEmail(String email) { this.identifier = email; }
    public String getEmail() { return this.identifier; }
}