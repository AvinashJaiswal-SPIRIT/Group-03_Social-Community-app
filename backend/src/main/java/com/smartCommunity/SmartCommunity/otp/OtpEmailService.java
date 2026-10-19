package com.smartCommunity.SmartCommunity.otp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpEmailService {

    private final JavaMailSender mailSender;
    private final String sender;

    public OtpEmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String sender
    ) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    public void sendOtp(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(sender);
        message.setTo(email);
        message.setSubject("Smart Community - Email Verification");
        message.setText(
                "Your OTP is " + otp
                        + "\nIt expires in 5 minutes."
        );

        mailSender.send(message);
    }
}

