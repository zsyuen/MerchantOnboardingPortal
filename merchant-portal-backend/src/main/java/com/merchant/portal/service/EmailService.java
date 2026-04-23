package com.merchant.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendApplicationConfirmation(String toEmail, String applicantName, String referenceId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Application Received – Merchant Portal (" + referenceId + ")");

            String htmlContent = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<meta charset=\"UTF-8\">\n" +
                    "</head>\n" +
                    "<body style=\"margin:0;padding:0;background-color:#ffffff;font-family:Arial,sans-serif;\">\n" +
                    "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                    "  <tr>\n" +
                    "    <td align=\"center\" style=\"padding:40px 20px;\">\n" +
                    "      <table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:600px;width:100%;\">\n" +
                    "        <tr>\n" +
                    "          <td style=\"background-color:#0056b3;padding:30px 30px 20px 30px;border-radius:8px 8px 0 0;\">\n" +
                    "            <h2 style=\"margin:0;color:#ffffff;font-size:24px;\">Application Received</h2>\n" +
                    "          </td>\n" +
                    "        </tr>\n" +
                    "        <tr>\n" +
                    "          <td style=\"background-color:#f9f9f9;padding:30px;border:1px solid #e0e0e0;\">\n" +
                    "            <p style=\"color:#333333;font-size:15px;\">Dear <strong>" + applicantName + "</strong>,</p>\n" +
                    "            <p style=\"color:#333333;font-size:15px;line-height:1.6;\">Thank you for submitting your merchant onboarding application. We have successfully received your details and our team is currently reviewing them.</p>\n" +
                    "            <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin:20px 0;\">\n" +
                    "              <tr>\n" +
                    "                <td style=\"background-color:#eef3fb;border-left:4px solid #0056b3;padding:20px 24px;border-radius:0 6px 6px 0;\">\n" +
                    "                  <p style=\"margin:0 0 6px 0;color:#555555;font-size:13px;\">Your Reference ID:</p>\n" +
                    "                  <p style=\"margin:0;color:#0056b3;font-size:22px;font-weight:bold;letter-spacing:1px;\">" + referenceId + "</p>\n" +
                    "                </td>\n" +
                    "              </tr>\n" +
                    "            </table>\n" +
                    "            <p style=\"color:#333333;font-size:15px;line-height:1.6;\">Please keep this Reference ID to track the status of your application on our portal.</p>\n" +
                    "            <p style=\"color:#333333;font-size:15px;line-height:1.6;\">Our review process typically takes 1–3 business days. We will get back to you shortly.</p>\n" +
                    "          </td>\n" +
                    "        </tr>\n" +
                    "        <tr>\n" +
                    "          <td style=\"background-color:#f0f0f0;padding:20px 30px;border-radius:0 0 8px 8px;border:1px solid #e0e0e0;border-top:none;\">\n" +
                    "            <p style=\"margin:0;color:#666666;font-size:13px;\">Best regards,<br><strong>Merchant Onboarding Team</strong></p>\n" +
                    "          </td>\n" +
                    "        </tr>\n" +
                    "      </table>\n" +
                    "    </td>\n" +
                    "  </tr>\n" +
                    "</table>\n" +
                    "</body>\n" +
                    "</html>";

            helper.setText(htmlContent, true); // Set to true to send as HTML
            mailSender.send(message);
            
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML confirmation email", e);
        }
    }
}