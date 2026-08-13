package com.pet.proj.coaching.application;

import com.pet.proj.ai.SemanticSearchService;
import com.pet.proj.coaching.api.KnowledgeDocumentPageResponse;
import com.pet.proj.coaching.api.KnowledgeDocumentResponse;
import com.pet.proj.coaching.persistence.KnowledgeDocumentEntity;
import com.pet.proj.coaching.persistence.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeDocumentService {
    private final KnowledgeDocumentRepository documentRepository;
    private final SemanticSearchService semanticSearchService;
    private final KnowledgeDocumentMapper mapper;

    @Value("${app.knowledge.similarity-threshold}")
    private double similarityThreshold;

    @Value("${app.knowledge.chunk-size:500}")
    private int chunkSize = 500;

    @Value("${app.knowledge.chunk-overlap:75}")
    private int chunkOverlap = 75;

    @Transactional
    public KnowledgeDocumentResponse create(MultipartFile file) {
        var parsed = parse(file);
        return mapper.toResponse(saveAndIndex(parsed));
    }

    public KnowledgeDocumentResponse get(UUID id) {
        return mapper.toResponse(documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("knowledge document not found")));
    }

    public KnowledgeDocumentPageResponse list(int limit, UUID cursor) {
        var entities = cursor == null
                ? documentRepository.findAllByOrderByIdAsc(PageRequest.of(0, limit + 1))
                : documentRepository.findByIdGreaterThanOrderByIdAsc(cursor, PageRequest.of(0, limit + 1));
        var hasMore = entities.size() > limit;
        var documents = entities.stream()
                .limit(limit)
                .map(mapper::toSummaryResponse)
                .toList();
        var nextCursor = hasMore ? documents.getLast().id() : null;
        return new KnowledgeDocumentPageResponse(documents, nextCursor, hasMore);
    }

    public String retrieveContext(String query, int limit) {
        try {
            return semanticSearchService.search(query, limit, similarityThreshold, "documentType == 'knowledge'")
                    .stream()
                    .map(document -> "Source: " + document.getMetadata().get("source")
                            + "\nTopic: " + document.getMetadata().get("topic")
                            + "\n" + document.getText())
                    .collect(Collectors.joining("\n\n---\n\n"));
        } catch (Exception e) {
            log.warn("Knowledge retrieval failed; continuing without knowledge context", e);
            return "";
        }
    }

    private void index(KnowledgeDocumentEntity document) {
        var chunks = split(document.getContent());
        var vectorDocuments = new ArrayList<Document>();
        for (var index = 0; index < chunks.size(); index++) {
            var chunkId = UUID.nameUUIDFromBytes(
                    (document.getId() + ":" + index).getBytes(StandardCharsets.UTF_8));
            vectorDocuments.add(new Document(
                    chunkId.toString(),
                    chunks.get(index),
                    Map.of(
                            "documentType", "knowledge",
                            "knowledgeDocumentId", document.getId().toString(),
                            "title", document.getTitle(),
                            "source", document.getSource(),
                            "topic", document.getTopic(),
                            "chunkIndex", index)));
        }
        semanticSearchService.replace(
                "documentType == 'knowledge' && knowledgeDocumentId == '" + document.getId() + "'",
                vectorDocuments);
    }

    private ParsedKnowledge parse(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("knowledge file is empty");
        }
        final String fileContent;
        try {
            fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("could not read knowledge file", e);
        }
        return parse(file.getOriginalFilename(), fileContent);
    }

    private ParsedKnowledge parse(String filename, String fileContent) {
        if (filename == null || !(filename.endsWith(".md") || filename.endsWith(".txt"))) {
            throw new IllegalArgumentException("only .md and .txt knowledge files are supported");
        }
        if (!fileContent.startsWith("---\n")) {
            throw new IllegalArgumentException("knowledge file must start with front matter");
        }
        var frontMatterEnd = fileContent.indexOf("\n---", 4);
        if (frontMatterEnd < 0) {
            throw new IllegalArgumentException("knowledge file has incomplete front matter");
        }

        var metadata = fileContent.substring(4, frontMatterEnd).lines()
                .map(line -> line.split(":", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> parts[0].trim(),
                        parts -> parts[1].trim()));
        var content = fileContent.substring(frontMatterEnd + 4).strip();
        if (content.isBlank()) {
            throw new IllegalArgumentException("knowledge file content is empty");
        }
        return new ParsedKnowledge(
                required(metadata, "title"),
                content,
                required(metadata, "source"),
                required(metadata, "topic"));
    }

    private KnowledgeDocumentEntity saveAndIndex(ParsedKnowledge parsed) {
        var document = documentRepository.save(KnowledgeDocumentEntity.builder()
                .title(parsed.title())
                .content(parsed.content())
                .source(parsed.source())
                .topic(parsed.topic())
                .build());
        index(document);
        return document;
    }

    private String required(Map<String, String> metadata, String name) {
        var value = metadata.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("knowledge file front matter requires '" + name + "'");
        }
        return value;
    }

    private List<String> split(String content) {
        var chunks = new ArrayList<String>();
        var start = 0;
        while (start < content.length()) {
            var end = Math.min(start + chunkSize, content.length());
            chunks.add(content.substring(start, end));
            if (end == content.length()) {
                break;
            }
            start = end - chunkOverlap;
        }
        return chunks;
    }

    private record ParsedKnowledge(String title, String content, String source, String topic) {
    }
}
