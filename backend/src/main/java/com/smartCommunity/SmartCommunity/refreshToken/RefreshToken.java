package com.smartCommunity.SmartCommunity.refreshToken;


import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
@Table(
        name = "refresh_tokens",
        indexes = @Index(
                name = "idx_refresh_token_hash",
                columnList = "tokenHash",
                unique = true
        )
)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    protected RefreshToken() {
    }

    public RefreshToken(
            String email,
            String tokenHash,
            Instant expiresAt
    ) {
        this.email = email;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}
