package com.pet.proj.coaching.api;

import java.util.UUID;

public record KnowledgeDocumentResponse(
        UUID id,
        String title,
        String content,
        String source,
        String topic
) {
}
