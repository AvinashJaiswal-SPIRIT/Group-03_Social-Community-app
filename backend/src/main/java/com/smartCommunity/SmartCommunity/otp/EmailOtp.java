package com.smartCommunity.SmartCommunity.otp;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

@Entity
@Getter
@Table(name = "email_otps")
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String otpHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts = 0;

    protected EmailOtp() {}

    public EmailOtp(String email, String otpHash, Instant expiresAt) {
        this.email = email;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}

