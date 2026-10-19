package com.smartCommunity.SmartCommunity.auth;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank
            @Email
            String email,

            @NotBlank
            @Size(min = 8, max = 72)
            String password,

            @NotBlank
            String name
    ) {
    }

    public record LoginRequest(
            @NotBlank
            @Email
            String email,

            @NotBlank
            String password
    ) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType
    ) {
    }
    public record VerifyOtpRequest(
            @Email
            @NotBlank
            String email,

            @Pattern(regexp = "\\d{6}")
            String otp
    ) {}

    public record ResendOtpRequest(
            @Email
            @NotBlank
            String email
    ) {}

    public record RefreshRequest(
            @NotBlank
            String refreshToken
    ) {}

    public record MessageResponse(
            String message
    ) {
    }
}


