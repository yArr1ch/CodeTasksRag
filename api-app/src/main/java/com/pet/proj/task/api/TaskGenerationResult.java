package com.pet.proj.task.api;

import com.pet.proj.task.domain.Task;

import java.util.List;
import java.util.UUID;

public record TaskGenerationResult(
        GenerationStatus status,
        Task task,
        List<SimilarTask> similarTasks,
        List<String> referenceSolutions
) {

    public record SimilarTask(
            UUID id,
            String title,
            String description,
            double similarity
    ) {
    }

    public enum GenerationStatus {
        SIMILAR_TASKS_FOUND,
        GENERATED
    }
}
