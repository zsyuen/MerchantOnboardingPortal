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
                    "<body bgcolor=\"#ffffff\">\n" +
                    "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                    "  <tr>\n" +
                    "    <td align=\"center\">\n" +
                    "      <table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"20\">\n" +
                    "        <tr>\n" +
                    "          <td align=\"left\">\n" +
                    "            <h2><font color=\"#0056b3\" face=\"Arial, sans-serif\">Application Received</font></h2>\n" +
                    "            <p><font color=\"#333333\" face=\"Arial, sans-serif\">Dear <b>" + applicantName + "</b>,</font></p>\n" +
                    "            <p><font color=\"#333333\" face=\"Arial, sans-serif\">Thank you for submitting your merchant onboarding application. We have successfully received your details and our team is currently reviewing them.</font></p>\n" +
                    "            <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" bgcolor=\"#f8f9fa\">\n" +
                    "              <tr>\n" +
                    "                <td width=\"4\" bgcolor=\"#0056b3\"></td>\n" +
                    "                <td cellpadding=\"15\">\n" +
                    "                  <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"15\">\n" +
                    "                    <tr>\n" +
                    "                      <td>\n" +
                    "                        <font color=\"#333333\" face=\"Arial, sans-serif\">Your Reference ID:<br>\n" +
                    "                        <b><font color=\"#0056b3\" size=\"5\">" + referenceId + "</font></b></font>\n" +
                    "                      </td>\n" +
                    "                    </tr>\n" +
                    "                  </table>\n" +
                    "                </td>\n" +
                    "              </tr>\n" +
                    "            </table>\n" +
                    "            <br>\n" +
                    "            <p><font color=\"#333333\" face=\"Arial, sans-serif\">Please keep this Reference ID to track the status of your application on our portal.</font></p>\n" +
                    "            <p><font color=\"#333333\" face=\"Arial, sans-serif\">Our review process typically takes 1-3 business days. We will get back to you shortly.</font></p>\n" +
                    "            <br>\n" +
                    "            <p><font color=\"#666666\" size=\"2\" face=\"Arial, sans-serif\">\n" +
                    "              Best regards,<br>\n" +
                    "              <b>Merchant Onboarding Team</b>\n" +
                    "            </font></p>\n" +
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