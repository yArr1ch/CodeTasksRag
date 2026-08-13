package com.pet.proj.task.api;

import com.pet.proj.task.domain.TaskGenerationStatus;

import java.util.List;
import java.util.UUID;

public record TaskGenerationResponse(
        UUID id,
        String prompt,
        TaskGenerationStatus status,
        int attempt,
        int maxAttempts,
        UUID taskId,
        List<TaskGenerationResult.SimilarTask> similarTasks,
        String error
) {
}
