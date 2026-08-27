package com.pet.proj.task.application;

import com.pet.proj.contracts.TaskGenerationRequestedEvent;
import com.pet.proj.task.api.TaskGenerationRequest;
import com.pet.proj.task.api.TaskGenerationResponse;
import com.pet.proj.task.domain.TaskGenerationStatus;
import com.pet.proj.task.persistence.TaskGenerationEntity;
import com.pet.proj.task.persistence.TaskGenerationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public final class TaskGenerationServiceImpl implements TaskGenerationService {
    private final TaskGenerationRepository taskGenerationRepository;
    private final TaskMapper taskMapper;
    private final TaskGenerationRequestPublisher taskGenerationRequestPublisher;
    private final TaskGenerationProcessor taskGenerationProcessor;

    @Value("${app.task-generation.max-attempts:3}")
    private int maxGenerationAttempts;

    @Value("${app.task-generation.timeout:10m}")
    private Duration generationTimeout;

    @Override
    public TaskGenerationResponse generate(TaskGenerationRequest request) {
        var generationId = UUID.randomUUID();
        var generation = taskGenerationRepository.save(TaskGenerationEntity.builder()
                .id(generationId)
                .prompt(request.prompt())
                .status(TaskGenerationStatus.GENERATING)
                .attempt(0)
                .maxAttempts(maxGenerationAttempts)
                .build());
        log.info("Task generation requested, generationId={}", generationId);

        taskGenerationRequestPublisher.publish(
                new TaskGenerationRequestedEvent(generationId)
        ).whenComplete((_, error) -> {
            if (error != null) {
                taskGenerationProcessor.markFailed(generationId, "task generation could not be queued");
            }
        });
        scheduleTimeout(generationId);
        return taskMapper.toGenerationResponse(generation);
    }

    @Override
    public TaskGenerationResponse getGeneration(UUID id) {
        return taskMapper.toGenerationResponse(getGenerationEntity(id));
    }

    @Override
    public void cancelGeneration(UUID id) {
        var generation = getGenerationEntity(id);
        if (!generation.isInProgress()) {
            throw new IllegalStateException("only in-progress task generations can be canceled");
        }
        generation.markCanceled();
        taskGenerationRepository.save(generation);
    }

    private void scheduleTimeout(UUID generationId) {
        CompletableFuture.delayedExecutor(generationTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .execute(() -> taskGenerationProcessor.markFailed(
                        generationId,
                        "AI task generation timed out after " + generationTimeout.toMinutes() + " minutes"));
    }

    private TaskGenerationEntity getGenerationEntity(UUID id) {
        return taskGenerationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("generation not found: " + id));
    }
}
