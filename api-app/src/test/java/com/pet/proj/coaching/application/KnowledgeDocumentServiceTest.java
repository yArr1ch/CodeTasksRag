package com.pet.proj.coaching.application;

import com.pet.proj.ai.SemanticSearchService;
import com.pet.proj.coaching.api.KnowledgeDocumentPageResponse;
import com.pet.proj.coaching.api.KnowledgeDocumentResponse;
import com.pet.proj.coaching.api.KnowledgeDocumentSummaryResponse;
import com.pet.proj.coaching.persistence.KnowledgeDocumentEntity;
import com.pet.proj.coaching.persistence.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentServiceTest {
    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private KnowledgeDocumentMapper mapper;

    private KnowledgeDocumentService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeDocumentService(documentRepository, semanticSearchService, mapper);
    }

    @Test
    void createsAndIndexesValidKnowledgeDocument() {
        var id = UUID.randomUUID();
        var saved = KnowledgeDocumentEntity.builder()
                .id(id)
                .title("AsterFold")
                .source("internal")
                .topic("algorithm")
                .content("Rotate the outer layer clockwise.")
                .build();
        when(documentRepository.save(any())).thenReturn(saved);
        var response = new KnowledgeDocumentResponse(id, "AsterFold", saved.getContent(), "internal", "algorithm");
        when(mapper.toResponse(saved)).thenReturn(response);

        var result = service.create(file("algo.md", "AsterFold", "internal", "algorithm",
                "Rotate the outer layer clockwise."));

        assertEquals(response, result);
        verify(semanticSearchService).replace(
                "documentType == 'knowledge' && knowledgeDocumentId == '" + id + "'",
                List.of(new Document(
                        UUID.nameUUIDFromBytes((id + ":0").getBytes(StandardCharsets.UTF_8)).toString(),
                        saved.getContent(),
                        java.util.Map.of(
                                "documentType", "knowledge",
                                "knowledgeDocumentId", id.toString(),
                                "title", "AsterFold",
                                "source", "internal",
                                "topic", "algorithm",
                                "chunkIndex", 0))));
    }

    @Test
    void rejectsEmptyDocumentBody() {
        var file = file("algo.md", "AsterFold", "internal", "algorithm", "   \n");

        var error = assertThrows(IllegalArgumentException.class, () -> service.create(file));

        assertEquals("knowledge file content is empty", error.getMessage());
    }

    @Test
    void returnsCursorWhenMoreDocumentsExist() {
        var first = document(UUID.randomUUID(), "First");
        var second = document(UUID.randomUUID(), "Second");
        var third = document(UUID.randomUUID(), "Third");
        when(documentRepository.findAllByOrderByIdAsc(any())).thenReturn(List.of(first, second, third));
        when(mapper.toSummaryResponse(first)).thenReturn(
                new KnowledgeDocumentSummaryResponse(first.getId(), first.getTitle(), first.getSource(), first.getTopic()));
        when(mapper.toSummaryResponse(second)).thenReturn(
                new KnowledgeDocumentSummaryResponse(second.getId(), second.getTitle(), second.getSource(), second.getTopic()));

        KnowledgeDocumentPageResponse page = service.list(2, null);

        assertEquals(List.of(
                new KnowledgeDocumentSummaryResponse(first.getId(), first.getTitle(), first.getSource(), first.getTopic()),
                new KnowledgeDocumentSummaryResponse(second.getId(), second.getTitle(), second.getSource(), second.getTopic())),
                page.documents());
        assertEquals(second.getId(), page.nextCursor());
        assertEquals(true, page.hasMore());
    }

    private KnowledgeDocumentEntity document(UUID id, String title) {
        return KnowledgeDocumentEntity.builder()
                .id(id)
                .title(title)
                .source("internal")
                .topic("algorithm")
                .content("Content")
                .build();
    }

    private MockMultipartFile file(String filename, String title, String source, String topic, String content) {
        return new MockMultipartFile("file", filename, "text/markdown",
                fileContent(title, source, topic, content).getBytes(StandardCharsets.UTF_8));
    }

    private String fileContent(String title, String source, String topic, String content) {
        return "---\n" +
                "title: " + title + "\n" +
                "source: " + source + "\n" +
                "topic: " + topic + "\n" +
                "---\n\n" + content;
    }
}
