package com.pet.proj.task.application;

import com.pet.proj.contracts.TaskGenerationRequestedEvent;

import java.util.UUID;

public interface TaskGenerationProcessor {
    void processGeneration(TaskGenerationRequestedEvent event);

    void handleDeadLetter(
            TaskGenerationRequestedEvent event,
            Exception failure);

    void markFailed(UUID generationId, String message);
}
