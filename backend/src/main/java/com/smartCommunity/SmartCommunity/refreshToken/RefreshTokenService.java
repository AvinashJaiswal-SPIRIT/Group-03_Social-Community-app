package com.smartCommunity.SmartCommunity.refreshToken;


import com.smartCommunity.SmartCommunity.users.AppUser;
import com.smartCommunity.SmartCommunity.users.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository tokenRepository,
            UserRepository userRepository
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to hash refresh token",
                    exception
            );
        }
    }

    @Transactional
    public String create(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid account"
                ));

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Email verification required"
            );
        }

        String rawToken = generateRawToken();
        RefreshToken record = new RefreshToken(
                email,
                hashToken(rawToken),
                Instant.now().plus(7, ChronoUnit.DAYS)
        );

        tokenRepository.save(record);
        return rawToken;
    }

    @Transactional
    public RotatedToken rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }

        String hash = hashToken(rawToken);
        RefreshToken oldToken = tokenRepository
                .findByHashForUpdate(hash)
                .orElseThrow(this::invalidToken);

        if (!oldToken.getExpiresAt().isAfter(Instant.now())) {
            tokenRepository.delete(oldToken);
            throw invalidToken();
        }

        String email = oldToken.getEmail();

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(this::invalidToken);

        if (!user.isEmailVerified()) {
            tokenRepository.delete(oldToken);
            throw invalidToken();
        }

        tokenRepository.delete(oldToken);
        tokenRepository.flush();

        String newRawToken = generateRawToken();
        tokenRepository.save(new RefreshToken(
                email,
                hashToken(newRawToken),
                Instant.now().plus(7, ChronoUnit.DAYS)
        ));
        return new RotatedToken(email, newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        tokenRepository.deleteByTokenHash(hashToken(rawToken));
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid or expired refresh token"
        );
    }

    public record RotatedToken(
            String email,
            String refreshToken
    ) {}
}
