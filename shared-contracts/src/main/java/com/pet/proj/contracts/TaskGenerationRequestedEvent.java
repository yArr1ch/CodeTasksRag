package com.pet.proj.contracts;

import java.time.Instant;
import java.util.UUID;

public record TaskGenerationRequestedEvent(
        UUID eventId,
        int schemaVersion,
        UUID generationId,
        Instant createdAt) {

    public TaskGenerationRequestedEvent(UUID generationId) {
        this(UUID.randomUUID(), 1, generationId, Instant.now());
    }
}
