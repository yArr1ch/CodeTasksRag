package com.pet.proj.coaching.api;

import java.util.UUID;

public record KnowledgeDocumentSummaryResponse(
        UUID id,
        String title,
        String source,
        String topic
) {
}
