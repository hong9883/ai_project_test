package com.rag.document.service;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final RestHighLevelClient client;
    private final Gson gson;

    @Value("${opensearch.index}")
    private String indexName;

    @Value("${ollama.embedding-dimension}")
    private int embeddingDimension;

    @PostConstruct
    public void initialize() {
        try {
            if (!indexExists()) {
                createIndex();
            }
        } catch (IOException e) {
            log.error("Failed to initialize OpenSearch index", e);
        }
    }

    private boolean indexExists() throws IOException {
        GetIndexRequest request = new GetIndexRequest(indexName);
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    private void createIndex() throws IOException {
        log.info("Creating OpenSearch index: {}", indexName);

        String mappings = String.format("""
            {
              "mappings": {
                "properties": {
                  "documentId": { "type": "long" },
                  "userId": { "type": "long" },
                  "chunkIndex": { "type": "integer" },
                  "text": { "type": "text" },
                  "embedding": {
                    "type": "knn_vector",
                    "dimension": %d
                  },
                  "metadata": { "type": "object" },
                  "createdAt": { "type": "date" }
                }
              },
              "settings": {
                "index": {
                  "knn": true,
                  "knn.algo_param.ef_search": 100
                }
              }
            }
            """, embeddingDimension);

        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.source(mappings, XContentType.JSON);

        client.indices().create(request, RequestOptions.DEFAULT);
        log.info("Index created successfully");
    }

    public String storeVector(Long documentId, Long userId, int chunkIndex,
                              String text, List<Double> embedding,
                              Map<String, Object> metadata) throws IOException {
        Map<String, Object> document = new HashMap<>();
        document.put("documentId", documentId);
        document.put("userId", userId);
        document.put("chunkIndex", chunkIndex);
        document.put("text", text);
        document.put("embedding", embedding);
        document.put("metadata", metadata);
        document.put("createdAt", new Date());

        IndexRequest request = new IndexRequest(indexName)
                .source(gson.toJson(document), XContentType.JSON);

        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
        log.debug("Stored vector with ID: {}", response.getId());

        return response.getId();
    }

    public List<Map<String, Object>> searchSimilar(List<Double> queryEmbedding, int topK) throws IOException {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(topK);

        // Note: For actual k-NN search, you would use a k-NN query plugin
        // This is a simplified version using match_all
        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.source(searchSourceBuilder);

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        List<Map<String, Object>> results = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            Map<String, Object> result = hit.getSourceAsMap();
            result.put("score", hit.getScore());
            results.add(result);
        }

        return results;
    }

    public VectorStoreService(RestHighLevelClient client) {
        this.client = client;
        this.gson = new Gson();
    }
}
