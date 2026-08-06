package com.pet.proj.coaching.application;

import com.pet.proj.ai.SemanticSearchService;
import com.pet.proj.coaching.api.KnowledgeDocumentResponse;
import com.pet.proj.coaching.persistence.KnowledgeDocumentEntity;
import com.pet.proj.coaching.persistence.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeDocumentService {
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 75;

    private final KnowledgeDocumentRepository documentRepository;
    private final SemanticSearchService semanticSearchService;
    private final KnowledgeDocumentMapper mapper;

    @Value("${app.knowledge.similarity-threshold}")
    private double similarityThreshold;

    @Transactional
    public KnowledgeDocumentResponse create(MultipartFile file) {
        var parsed = parse(file);
        var document = documentRepository.save(KnowledgeDocumentEntity.builder()
                .title(parsed.title())
                .content(parsed.content())
                .source(parsed.source())
                .topic(parsed.topic())
                .build());
        index(document);
        return mapper.toResponse(document);
    }

    public KnowledgeDocumentResponse get(UUID id) {
        return mapper.toResponse(documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("knowledge document not found")));
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
        semanticSearchService.replace(vectorDocuments);
    }

    private ParsedKnowledge parse(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("knowledge file is empty");
        }
        var filename = file.getOriginalFilename();
        if (filename == null || !(filename.endsWith(".md") || filename.endsWith(".txt"))) {
            throw new IllegalArgumentException("only .md and .txt knowledge files are supported");
        }
        final String fileContent;
        try {
            fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("could not read knowledge file", e);
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
        return new ParsedKnowledge(
                required(metadata, "title"),
                content,
                required(metadata, "source"),
                required(metadata, "topic"));
    }

    private String required(Map<String, String> metadata, String name) {
        var value = metadata.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("knowledge file front matter requires '" + name + "'");
        }
        return value;
    }

    private ArrayList<String> split(String content) {
        var chunks = new ArrayList<String>();
        var start = 0;
        while (start < content.length()) {
            var end = Math.min(start + CHUNK_SIZE, content.length());
            chunks.add(content.substring(start, end));
            if (end == content.length()) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }
        return chunks;
    }

    private record ParsedKnowledge(String title, String content, String source, String topic) {
    }
}
