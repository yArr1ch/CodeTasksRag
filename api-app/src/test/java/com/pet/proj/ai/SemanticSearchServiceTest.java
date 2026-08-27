package com.pet.proj.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {
    @Mock
    private VectorStore vectorStore;

    @Test
    void replacesAllDocumentsMatchingLogicalSet() {
        var service = new SemanticSearchService(vectorStore);
        var documents = List.of(new Document("content", Map.of("id", "chunk-1")));

        service.replace("documentType == 'knowledge'", documents);

        verify(vectorStore).delete("documentType == 'knowledge'");
        verify(vectorStore).add(documents);
    }

    @Test
    void removesLogicalSetWhenReplacementIsEmpty() {
        var service = new SemanticSearchService(vectorStore);

        service.replace("documentType == 'solution'", List.of());

        verify(vectorStore).delete("documentType == 'solution'");
        verify(vectorStore, never()).add(List.of());
    }
}
