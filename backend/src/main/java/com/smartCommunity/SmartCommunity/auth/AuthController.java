package com.smartCommunity.SmartCommunity.auth;


import com.smartCommunity.SmartCommunity.refreshToken.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService, UserDetailsService userDetailsService, JwtService jwtService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;

        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.MessageResponse register(
            @Valid @RequestBody AuthDtos.RegisterRequest request) {

        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(
            @Valid @RequestBody AuthDtos.LoginRequest request) {

        return authService.login(request);
    }
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthDtos.MessageResponse> verifyOtp(
            @Valid @RequestBody AuthDtos.VerifyOtpRequest request
    ) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<AuthDtos.MessageResponse> resendOtp(
            @Valid @RequestBody AuthDtos.ResendOtpRequest request
    ) {
        return ResponseEntity.ok(authService.resendOtp(request));
    }

    @PostMapping("/refresh")
    public AuthDtos.AuthResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(request.refreshToken());
        var userDetails = userDetailsService.loadUserByUsername(rotated.email());

        String newAccessToken = jwtService.generateToken(userDetails);
        return new AuthDtos.AuthResponse(
                newAccessToken,
                rotated.refreshToken(),
                "Bearer"
        );
    }
    @PostMapping("/logout")
    public AuthDtos.MessageResponse logout(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        return new AuthDtos.MessageResponse("Logged out successfully");
    }
}
