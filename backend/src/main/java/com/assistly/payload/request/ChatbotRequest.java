package com.assistly.payload.request;

import jakarta.validation.constraints.NotBlank;

public class ChatbotRequest {
    @NotBlank
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
