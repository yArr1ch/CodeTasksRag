package com.pet.proj.coaching.api;

import java.util.List;
import java.util.UUID;

public record KnowledgeDocumentPageResponse(
        List<KnowledgeDocumentSummaryResponse> documents,
        UUID nextCursor,
        boolean hasMore
) {
}
