package com.rag.chat.controller;

import com.rag.chat.dto.ChatHistoryResponse;
import com.rag.chat.dto.ChatRequest;
import com.rag.chat.dto.ChatResponse;
import com.rag.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<?> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            ChatResponse response = chatService.chat(request, userId);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to process chat request", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process chat request"));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatHistoryResponse>> getChatHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String sessionId) {
        List<ChatHistoryResponse> history = chatService.getChatHistory(userId, sessionId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "chat-service"));
    }
}
