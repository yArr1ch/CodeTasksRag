package com.pet.proj.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubmissionCompletedEvent(UUID eventId, int schemaVersion, UUID correlationId,
                                       UUID submissionId, UUID taskId, String status, int passedTests,
                                       int totalTests, String error, String executionMode,
                                       List<String> outputs, Instant completedAt) {

    public SubmissionCompletedEvent(UUID correlationId, UUID submissionId, UUID taskId, String status,
                                    int passedTests, int totalTests, String error, String executionMode,
                                    List<String> outputs) {
        this(UUID.randomUUID(), 2, correlationId, submissionId, taskId, status, passedTests,
                totalTests, error, executionMode, outputs == null ? List.of() : outputs, Instant.now());
    }
}
