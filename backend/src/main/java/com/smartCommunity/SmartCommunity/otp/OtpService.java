package com.smartCommunity.SmartCommunity.otp;


import com.smartCommunity.SmartCommunity.users.AppUser;
import com.smartCommunity.SmartCommunity.users.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class OtpService {

    private final EmailOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpEmailService emailService;

    private final SecureRandom random = new SecureRandom();

    public OtpService(
            EmailOtpRepository otpRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OtpEmailService emailService
    ) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void generateAndSend(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email already verified");
        }
        String otp = String.format(
                "%06d",
                random.nextInt(1_000_000)
        );
        EmailOtp record = new EmailOtp(
                email,
                passwordEncoder.encode(otp),
                Instant.now().plus(5, ChronoUnit.MINUTES)
        );

        otpRepository.deleteByEmail(email);
        otpRepository.save(record);
        emailService.sendOtp(email, otp);
    }

    @Transactional
    public boolean verify(String email, String otp) {
        EmailOtp record = otpRepository.findByEmail(email)
                .orElse(null);

        if (record == null) {
            return false;
        }
        if (record.getExpiresAt().isBefore(Instant.now())) {
            otpRepository.delete(record);
            return false;
        }
        if (record.getAttempts() >= 5) {
            otpRepository.delete(record);
            return false;
        }
        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            record.incrementAttempts();
            otpRepository.save(record);
            return false;
        }
        AppUser user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return false;
        }
        user.setEmailVerified(true);
        userRepository.save(user);
        otpRepository.delete(record);

        return true;
    }

}

