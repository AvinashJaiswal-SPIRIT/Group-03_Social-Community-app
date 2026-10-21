package com.smartCommunity.SmartCommunity.otp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OtpEmailService {

    private static final String BREVO_URL =
            "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate;

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${app.mail.sender}")
    private String senderEmail;

    @Value("${app.mail.sender-name}")
    private String senderName;

    public OtpEmailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendOtpEmail(String recipientEmail, String otp) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("api-key", apiKey);

        Map<String, Object> requestBody = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", senderEmail
                ),
                "to", List.of(Map.of(
                        "email", recipientEmail
                )),
                "subject", "SmartCommunity Email Verification",
                "textContent",
                "Your SmartCommunity OTP is " + otp
                        + ". It expires in 5 minutes. Do not share it.",
                "htmlContent",
                "<html><body>"
                        + "<h2>SmartCommunity Verification</h2>"
                        + "<p>Your OTP is:</p>"
                        + "<h1>" + otp + "</h1>"
                        + "<p>This code expires in 5 minutes. "
                        + "Do not share it with anyone.</p>"
                        + "</body></html>"
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.postForEntity(
                    BREVO_URL,
                    request,
                    String.class
            );
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Brevo rejected the email request. HTTP status: "
                            + exception.getStatusCode().value(),
                    exception
            );
        }
    }
}