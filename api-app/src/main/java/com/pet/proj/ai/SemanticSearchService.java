package com.pet.proj.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {
    private final VectorStore vectorStore;

    public List<Document> search(String query, int limit, double threshold, String filter) {
        var request = SearchRequest.builder()
                .query(query)
                .topK(limit)
                .similarityThreshold(threshold);
        if (filter != null && !filter.isBlank()) {
            request.filterExpression(filter);
        }
        return vectorStore.similaritySearch(request.build());
    }

    public void replace(List<Document> documents) {
        if (documents.isEmpty()) {
            return;
        }
        vectorStore.delete(documents.stream().map(Document::getId).toList());
        vectorStore.add(documents);
    }

    public void replace(String filter, List<Document> documents) {
        vectorStore.delete(filter);
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
    }

    public double score(Document document) {
        if (document.getScore() != null) {
            return document.getScore();
        }
        var distance = document.getMetadata().get("distance");
        if (distance != null) {
            try {
                return 1.0 - Double.parseDouble(distance.toString());
            } catch (NumberFormatException ignored) {
                log.warn("Could not parse vector distance '{}' for document {}", distance, document.getId());
            }
        }
        return 0.0;
    }
}
