package com.rag.chat.service;

import com.rag.chat.dto.ChatHistoryResponse;
import com.rag.chat.dto.ChatRequest;
import com.rag.chat.dto.ChatResponse;
import com.rag.chat.entity.ChatMessage;
import com.rag.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final RagService ragService;

    @Value("${rag.max-history}")
    private int maxHistory;

    @Transactional
    public ChatResponse chat(ChatRequest request, Long userId) throws IOException {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        log.info("Processing chat request for user {} in session {}", userId, sessionId);

        // Save user message
        ChatMessage userMessage = ChatMessage.builder()
                .userId(userId)
                .sessionId(sessionId)
                .type(ChatMessage.MessageType.USER)
                .content(request.getMessage())
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(userMessage);

        // Retrieve chat history
        List<ChatMessage> history = messageRepository
                .findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId);

        List<Map<String, String>> chatHistory = history.stream()
                .limit(maxHistory)
                .map(msg -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("role", msg.getType().name());
                    m.put("content", msg.getContent());
                    return m;
                })
                .collect(Collectors.toList());

        // Retrieve relevant context
        Map<String, Object> contextResult = ragService.retrieveContext(request.getMessage());
        List<String> contexts = (List<String>) contextResult.get("contexts");
        List<String> sources = (List<String>) contextResult.get("sources");

        // Generate answer
        String answer = ragService.generateAnswer(request.getMessage(), contexts, chatHistory);

        // Save assistant message
        ChatMessage assistantMessage = ChatMessage.builder()
                .userId(userId)
                .sessionId(sessionId)
                .type(ChatMessage.MessageType.ASSISTANT)
                .content(answer)
                .context(String.join("\n\n---\n\n", contexts))
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(assistantMessage);

        log.info("Chat response generated for user {} in session {}", userId, sessionId);

        return ChatResponse.builder()
                .sessionId(sessionId)
                .response(answer)
                .sources(sources.stream().distinct().collect(Collectors.toList()))
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> getChatHistory(Long userId, String sessionId) {
        List<ChatMessage> messages;

        if (sessionId != null && !sessionId.isEmpty()) {
            messages = messageRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId);
        } else {
            messages = messageRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        return messages.stream()
                .map(msg -> ChatHistoryResponse.builder()
                        .id(msg.getId())
                        .type(msg.getType().name())
                        .content(msg.getContent())
                        .timestamp(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
