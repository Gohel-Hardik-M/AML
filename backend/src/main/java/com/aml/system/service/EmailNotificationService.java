package com.aml.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOnboardingEmail(String toEmail, String bankName, String tempPassword) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@aml-compliance-platform.com");
        message.setTo(toEmail);
        message.setSubject("Welcome to AML Platform - " + bankName);
        message.setText(
                "Your institution has been successfully onboarded.\n\n" +
                "Your temporary password is: " + tempPassword + "\n\n" +
                "You will be required to change this upon your first login.\n" +
                "Do not share this email."
        );

        sendWithRetry(message, toEmail);
    }


    public void sendOfficerWelcomeEmail(String toEmail, String fullName,
                                         String username, String tempPassword,
                                         String tenantId) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@aml-compliance-platform.com");
        message.setTo(toEmail);
        message.setSubject("AML Platform - Compliance Officer Account Created");
        message.setText(
                "Hello " + fullName + ",\n\n" +
                "Your Compliance Officer account has been created.\n\n" +
                "Your login details:\n" +
                "  Tenant ID: " + tenantId + "\n" +
                "  Username: " + username + "\n" +
                "  Temporary Password: " + tempPassword + "\n\n" +
                "You will be required to change this password upon your first login.\n" +
                "Do not share this email."
        );


        sendWithRetry(message, toEmail);
    }

    public void sendComplianceOfficerWelcomeEmail(String toEmail, String fullName,
                                                 String tenantId, String username,
                                                 String tempPassword) {
        sendOfficerWelcomeEmail(toEmail, fullName, username, tempPassword, tenantId);
    }

    private void sendWithRetry(SimpleMailMessage message, String toEmail) {
        Exception lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                mailSender.send(message);
                log.info("Email sent successfully to {} on attempt {}", toEmail, attempt);
                return; // Success! Exit the method.

            } catch (Exception e) {
                lastError = e;
                log.warn("Email attempt {} of {} failed for {}: {}",
                        attempt, MAX_RETRIES, toEmail, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // All retries failed — throw error so the caller can rollback
        log.error("All {} email attempts failed for {}", MAX_RETRIES, toEmail);
        throw new RuntimeException(
                "Failed to send email to " + toEmail + " after " + MAX_RETRIES + " attempts. " +
                "Last error: " + (lastError != null ? lastError.getMessage() : "unknown"),
                lastError
        );
    }
}