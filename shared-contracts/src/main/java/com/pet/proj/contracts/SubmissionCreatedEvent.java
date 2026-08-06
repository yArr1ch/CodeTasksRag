package com.pet.proj.contracts;

import java.time.Instant;
import java.util.UUID;

public record SubmissionCreatedEvent(UUID eventId, int schemaVersion, UUID correlationId,
                                     UUID submissionId, UUID taskId, String executionMode, Instant createdAt) {
    public static final String STANDARD = "STANDARD";
    public static final String REFERENCE_ORACLE = "REFERENCE_ORACLE";

    public SubmissionCreatedEvent(UUID correlationId, UUID submissionId, UUID taskId) {
        this(correlationId, submissionId, taskId, STANDARD);
    }

    public SubmissionCreatedEvent(UUID correlationId, UUID submissionId, UUID taskId, String executionMode) {
        this(UUID.randomUUID(), 2, correlationId, submissionId, taskId, executionMode, Instant.now());
    }
}
