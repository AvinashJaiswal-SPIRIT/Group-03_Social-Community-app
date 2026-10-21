package com.smartCommunity.SmartCommunity.otp;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;

@Service
public class OtpService {

    private static final long OTP_VALIDITY_SECONDS = 300;
    private static final int MAX_ATTEMPTS = 5;

    private final EmailOtpRepository otpRepository;
    private final OtpEmailService otpEmailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public OtpService(
            EmailOtpRepository otpRepository,
            OtpEmailService otpEmailService) {
        this.otpRepository = otpRepository;
        this.otpEmailService = otpEmailService;
    }

    @Transactional
    public void generateOtp(String email) {
        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        String otpHash = passwordEncoder.encode(otp);

        EmailOtp emailOtp = new EmailOtp(
                email,
                otpHash,
                Instant.now().plusSeconds(OTP_VALIDITY_SECONDS)
        );

        // Replace any previous OTP for this email.
        otpRepository.deleteByEmail(email);
        otpRepository.save(emailOtp);

        try {
            // Send the plain OTP by email, never the stored hash.
            otpEmailService.sendOtpEmail(email, otp);
        } catch (RuntimeException exception) {
            otpRepository.deleteByEmail(email);
            throw exception;
        }
    }

    @Transactional
    public boolean verifyOtp(String email, String submittedOtp) {
        EmailOtp savedOtp = otpRepository.findByEmail(email)
                .orElse(null);

        if (savedOtp == null) {
            return false;
        }

        if (savedOtp.getExpiresAt().isBefore(Instant.now())) {
            otpRepository.deleteByEmail(email);
            return false;
        }

        if (savedOtp.getAttempts() >= MAX_ATTEMPTS) {
            otpRepository.deleteByEmail(email);
            return false;
        }

        if (submittedOtp == null
                || !submittedOtp.matches("\\d{6}")) {
            savedOtp.incrementAttempts();
            return false;
        }

        boolean matches = passwordEncoder.matches(
                submittedOtp,
                savedOtp.getOtpHash()
        );

        if (!matches) {
            savedOtp.incrementAttempts();
            return false;
        }

        // A valid OTP is single-use.
        otpRepository.deleteByEmail(email);
        return true;
    }
}

