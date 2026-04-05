package com.assistly.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@assistly.local}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetConfirmation(String toEmail) {
        sendEmail(
                toEmail,
                "Assistly Password Reset Successful",
                "Your Assistly account password has been reset successfully.\n\n"
                        + "If you did not perform this action, contact support immediately."
        );
    }

    public void sendSignupWelcome(String toEmail, String name) {
        String safeName = (name == null || name.isBlank()) ? "there" : name;
        sendEmail(
                toEmail,
                "Welcome to Assistly",
                "Hi " + safeName + ",\n\n"
                        + "Welcome to Assistly. Your account is now active.\n"
                        + "You can sign in and start using community features right away."
        );
    }

    private void sendEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            // SMTP issues should not block auth flows.
            LOGGER.warn("Unable to send email to {}: {}", toEmail, ex.getMessage());
        }
    }
}
