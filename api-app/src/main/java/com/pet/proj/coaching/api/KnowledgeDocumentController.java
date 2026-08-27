package com.pet.proj.coaching.api;

import com.pet.proj.coaching.application.KnowledgeDocumentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class KnowledgeDocumentController {
    private final KnowledgeDocumentService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeDocumentResponse create(@RequestPart("file") MultipartFile file) {
        return service.create(file);
    }

    @GetMapping("/{id}")
    public KnowledgeDocumentResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public KnowledgeDocumentPageResponse list(
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int limit,
            @RequestParam(required = false) UUID cursor) {
        return service.list(limit, cursor);
    }
}
