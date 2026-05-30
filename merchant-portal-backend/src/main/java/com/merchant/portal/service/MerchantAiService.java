package com.merchant.portal.service;

import com.merchant.portal.model.Application;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class MerchantAiService {

    private static final String BANKING_COMPLIANCE_PROMPT = """
            You are a professional banking compliance officer.

            Review the merchant application below and return exactly 3 sentences suitable for an Officer's Dashboard.
            Business Name: {businessName}
            Status: {status}
            Face Match Score: {score}

            Instructions:
            - Analyze the application for potential risks based on the status and business name.
            - If the faceMatchScore is below 0.7, explicitly recommend Manual Review.
            - Keep the tone concise, professional, and decision-oriented.
            """;

    private final ChatClient chatClient;

    public MerchantAiService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        ChatClient client = null;
        try {
            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder != null) {
                try {
                    client = builder.build();
                } catch (Exception e) {
                    // Builder existed but failed to build (e.g. missing credentials). Log and continue with fallback.
                    log.warn("ChatClient.Builder was present but failed to build ChatClient - AI features will be disabled in this environment", e);
                }
            }
        } catch (Exception e) {
            // Defensive: any unexpected error retrieving the provider should not fail application startup.
            log.warn("Unable to obtain ChatClient.Builder - AI features will be disabled in this environment", e);
        }
        this.chatClient = client;
    }

    public String analyzeMerchantApplication(Application app, double faceMatchScore) {
        String businessName = firstNonBlank(app.getCompanyName(), app.getMerchantNameEn(), "Unknown business");
        String status = firstNonBlank(app.getStatus(), app.getVerificationStatus(), "Unknown");
        String score = String.format(Locale.US, "%.2f", faceMatchScore);

        // If chat client is not configured (e.g. in tests), return a deterministic fallback
        if (this.chatClient == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("[AI unavailable] ");
            if (faceMatchScore < 0.7d) {
                sb.append("Manual Review recommended. ");
            }
            sb.append(String.format("%s — Status: %s. Face match score: %s.", businessName, status, score));
            return sb.toString();
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("businessName", businessName);
        variables.put("status", status);
        variables.put("score", score);

        PromptTemplate promptTemplate = new PromptTemplate(BANKING_COMPLIANCE_PROMPT);
        String prompt = promptTemplate.render(variables);

        if (faceMatchScore < 0.7d) {
            prompt = prompt + "\n\nThe face match score is below 0.7, so the response must explicitly recommend Manual Review.";
        }

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}


