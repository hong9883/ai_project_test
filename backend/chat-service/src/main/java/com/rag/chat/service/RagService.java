package com.rag.chat.service;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final RestHighLevelClient openSearchClient;
    private final OllamaService ollamaService;
    private final Gson gson = new Gson();

    @Value("${opensearch.index}")
    private String indexName;

    @Value("${rag.top-k}")
    private int topK;

    public Map<String, Object> retrieveContext(String query) throws IOException {
        log.info("Retrieving context for query: {}", query);

        // Generate embedding for the query
        List<Double> queryEmbedding = ollamaService.generateEmbedding(query);

        // Search for similar documents
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(topK);

        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.source(searchSourceBuilder);

        SearchResponse response = openSearchClient.search(searchRequest, RequestOptions.DEFAULT);

        List<String> contexts = new ArrayList<>();
        List<String> sources = new ArrayList<>();

        for (SearchHit hit : response.getHits().getHits()) {
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            String text = (String) sourceMap.get("text");
            if (text != null) {
                contexts.add(text);

                Map<String, Object> metadata = (Map<String, Object>) sourceMap.get("metadata");
                if (metadata != null && metadata.containsKey("originalFilename")) {
                    sources.add((String) metadata.get("originalFilename"));
                }
            }
        }

        log.info("Retrieved {} context chunks", contexts.size());

        Map<String, Object> result = new HashMap<>();
        result.put("contexts", contexts);
        result.put("sources", sources);
        return result;
    }

    public String generateAnswer(String query, List<String> contexts, List<Map<String, String>> chatHistory) throws IOException {
        log.info("Generating answer for query with {} contexts", contexts.size());

        // Build prompt with context and history
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are a helpful AI assistant. ");
        promptBuilder.append("Answer the following question based on the provided context.\n\n");

        if (!contexts.isEmpty()) {
            promptBuilder.append("Context:\n");
            for (int i = 0; i < contexts.size(); i++) {
                promptBuilder.append(String.format("[%d] %s\n\n", i + 1, contexts.get(i)));
            }
        }

        if (chatHistory != null && !chatHistory.isEmpty()) {
            promptBuilder.append("\nChat History:\n");
            for (Map<String, String> msg : chatHistory) {
                promptBuilder.append(String.format("%s: %s\n", msg.get("role"), msg.get("content")));
            }
        }

        promptBuilder.append("\nQuestion: ").append(query).append("\n\n");
        promptBuilder.append("Answer: ");

        String prompt = promptBuilder.toString();
        log.debug("Generated prompt length: {}", prompt.length());

        return ollamaService.generateResponse(prompt);
    }
}
