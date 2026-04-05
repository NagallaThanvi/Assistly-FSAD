package com.assistly.controller;

import com.assistly.payload.request.ChatbotRequest;
import com.assistly.payload.response.ChatbotResponse;
import com.assistly.services.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public ResponseEntity<ChatbotResponse> message(@Valid @RequestBody ChatbotRequest request) {
        String reply = chatbotService.generateReply(request.getMessage());
        return ResponseEntity.ok(new ChatbotResponse(reply));
    }
}
