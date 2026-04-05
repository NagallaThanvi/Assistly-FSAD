package com.assistly.payload.response;

public class ChatbotResponse {
    private String reply;

    public ChatbotResponse(String reply) {
        this.reply = reply;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}
