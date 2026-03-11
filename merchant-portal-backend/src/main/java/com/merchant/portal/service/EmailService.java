package com.merchant.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendApplicationConfirmation(String toEmail, String applicantName, String referenceId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Merchant Portal – Application Received (" + referenceId + ")");
        message.setText(
                "Dear " + applicantName + ",\n\n" +
                        "Thank you for submitting your merchant onboarding application.\n\n" +
                        "Your application has been received and is currently under review.\n\n" +
                        "Your Reference ID: " + referenceId + "\n\n" +
                        "Please keep this Reference ID for your records. " +
                        "You may use it to track the status of your application.\n\n" +
                        "Our team will review your application and get back to you shortly.\n\n" +
                        "Best regards,\n" +
                        "Merchant Onboarding Team"
        );
        mailSender.send(message);
    }
}