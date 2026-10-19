package com.smartCommunity.SmartCommunity.auth;

import com.smartCommunity.SmartCommunity.auth.AuthDtos;
import com.smartCommunity.SmartCommunity.auth.JwtService;
import com.smartCommunity.SmartCommunity.otp.OtpService;
import com.smartCommunity.SmartCommunity.refreshToken.RefreshTokenService;
import com.smartCommunity.SmartCommunity.users.AppUser;
import com.smartCommunity.SmartCommunity.users.CustomUserDetailsService;
import com.smartCommunity.SmartCommunity.users.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService, OtpService otpService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthDtos.MessageResponse register(
            AuthDtos.RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered");
        }

        String hashedPassword = passwordEncoder.encode(request.password());

        AppUser user = new AppUser(email, hashedPassword);

        user.setEmailVerified(false);
        user.setDisplayName(request.name().trim()); // shown on the dashboard greeting
        userRepository.saveAndFlush(user);

        otpService.generateAndSend(email); // use the normalized (lowercase) email

        return new AuthDtos.MessageResponse(
                "Registration successful. Check your email for the OTP."
        );
    }

    public AuthDtos.AuthResponse login(
            AuthDtos.LoginRequest request) {

        String email = request.email().trim().toLowerCase();

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid email or password"));

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Verify your email before logging in");
        }

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                email,
                request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        String token = jwtService.generateToken(userDetails);
        String refreshToken = refreshTokenService.create(email);
        return new AuthDtos.AuthResponse(token,refreshToken ,"Bearer");
    }

    public AuthDtos.MessageResponse verifyOtp(AuthDtos.VerifyOtpRequest request) {

        boolean verified = otpService.verify(
                request.email(),
                request.otp()
        );

        if (!verified) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired OTP"
            );
        }
        return new AuthDtos.MessageResponse(
                "Email verified. You can now log in."
        );
    }
    public AuthDtos.MessageResponse resendOtp(AuthDtos.ResendOtpRequest request) {

        otpService.generateAndSend(request.email());

        return new AuthDtos.MessageResponse(
                "If the account is eligible, a verification code will be sent."
        );
    }
}

