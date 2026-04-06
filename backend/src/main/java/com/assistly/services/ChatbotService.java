package com.assistly.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ChatbotService {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${chatbot.webhook.url:}")
    private String webhookUrl;

    @Value("${chatbot.webhook.auth-token:}")
    private String webhookAuthToken;

    public ChatbotService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String generateReply(String userMessage) {
        String trimmed = userMessage == null ? "" : userMessage.trim();
        if (trimmed.isEmpty()) {
            return "Please type your question and I will help you.";
        }

        if (webhookUrl != null && !webhookUrl.isBlank()) {
            String webhookReply = fetchFromWebhook(trimmed);
            if (webhookReply != null && !webhookReply.isBlank()) {
                return webhookReply;
            }
        }

        return localFallback(trimmed);
    }

    private String fetchFromWebhook(String userMessage) {
        try {
            String payload = objectMapper.createObjectNode()
                    .put("message", userMessage)
                    .toString();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

            if (webhookAuthToken != null && !webhookAuthToken.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + webhookAuthToken.trim());
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode node = objectMapper.readTree(response.body());
                JsonNode replyNode = node.get("reply");
                return replyNode != null ? replyNode.asText() : null;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException | IllegalArgumentException ex) {
            return null;
        }
        return null;
    }

    private String localFallback(String message) {
        String lower = message.toLowerCase();

        if (lower.contains("signup") || lower.contains("register") || lower.contains("create account")) {
            return "Go to Signup, fill name/email/password, then submit. If you are an admin, complete the additional community setup step.";
        }

        if (lower.contains("login") || lower.contains("sign in") || lower.contains("google")) {
            return "Use the Login page with email/password or the Login with Google button. Make sure your Google client ID is set in frontend env.";
        }

        if (lower.contains("request") || lower.contains("volunteer") || lower.contains("community")) {
            return "Open the user dashboard, join a community, then create or accept requests. Requests include map location coordinates when selected.";
        }

        if (lower.contains("reset password") || lower.contains("forgot")) {
            return "Use Forgot Password from Login, provide your email and a new password. A confirmation email is sent after reset.";
        }

        return "I can help with signup/login, Google auth, password reset, communities, requests, and map usage. Ask a specific question.";
    }
}
