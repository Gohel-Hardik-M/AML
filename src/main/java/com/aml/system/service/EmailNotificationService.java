package com.aml.system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async // Crucial: Prevents the API from waiting for the SMTP server
    public void sendOnboardingEmail(String toEmail, String bankName, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@aml-compliance-platform.com");
            message.setTo(toEmail);
            message.setSubject("Welcome to AML Platform - " + bankName);
            message.setText("Your institution has been successfully onboarded.\n\n" +
                    "Your temporary password is: " + tempPassword + "\n\n" +
                    "You will be required to change this upon your first login. Do not share this email.");

            mailSender.send(message);
            log.info("Onboarding email sent asynchronously to {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send onboarding email to {}: {}", toEmail, e.getMessage());
        }
    }
}