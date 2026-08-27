package com.pet.proj.submission.domain;

import java.util.UUID;

public record Submission(UUID id, UUID taskId, String sourceCode, SubmissionStatus status,
                         int passedTests, int totalTests, String error) {
}
