package com.pet.proj.task.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.ai.OllamaProvider;
import com.pet.proj.coaching.application.KnowledgeDocumentService;
import com.pet.proj.contracts.TaskGenerationRequestedEvent;
import com.pet.proj.submission.application.SubmissionService;
import com.pet.proj.task.api.TaskGenerationRequest;
import com.pet.proj.task.api.TaskGenerationResponse;
import com.pet.proj.task.api.TaskGenerationResult;
import com.pet.proj.task.domain.TaskGenerationStatus;
import com.pet.proj.task.persistence.TaskGenerationEntity;
import com.pet.proj.task.persistence.TaskGenerationRepository;
import com.pet.proj.task.persistence.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskGenerationServiceImplTest {
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskGenerationRepository taskGenerationRepository;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private OllamaProvider aiProvider;
    @Mock
    private TaskSimilarityService similarityService;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private KnowledgeDocumentService knowledgeDocuments;
    @Mock
    private SubmissionService submissions;
    @Mock
    private KafkaTemplate<String, TaskGenerationRequestedEvent> taskGenerationEvents;

    @InjectMocks
    private TaskGenerationServiceImpl generationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(generationService, "generationTimeout", Duration.ofMinutes(10));
        ReflectionTestUtils.setField(generationService, "taskGenerationRequestedTopic", "task-generation-requested");
    }

    @Test
    void generate_kafkaPublishFailure_marksGenerationFailed() {
        var generation = new AtomicReference<TaskGenerationEntity>();
        when(taskGenerationRepository.save(any(TaskGenerationEntity.class))).thenAnswer(invocation -> {
            var saved = invocation.<TaskGenerationEntity>getArgument(0);
            generation.set(saved);
            return saved;
        });
        when(taskGenerationRepository.findById(any())).thenAnswer(invocation -> Optional.of(generation.get()));
        when(taskMapper.toGenerationResponse(any())).thenAnswer(invocation -> response(generation.get()));
        when(taskGenerationEvents.send(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        generationService.generate(new TaskGenerationRequest("prompt"));

        assertThat(generation.get().getStatus()).isEqualTo(TaskGenerationStatus.FAILED);
        assertThat(generation.get().getErrorMessage()).isEqualTo("task generation could not be queued");
    }

    @Test
    void generate_similarTaskFound_stopsBeforeAiCall() {
        var generation = new AtomicReference<TaskGenerationEntity>();
        var similar = new TaskGenerationResult.SimilarTask(
                UUID.randomUUID(), "Existing task", "Already implemented", 0.91);
        when(mapper.createArrayNode()).thenReturn(new ObjectMapper().createArrayNode());
        when(taskGenerationRepository.save(any(TaskGenerationEntity.class))).thenAnswer(invocation -> {
            var saved = invocation.<TaskGenerationEntity>getArgument(0);
            generation.set(saved);
            return saved;
        });
        when(taskGenerationRepository.findById(any())).thenAnswer(invocation -> Optional.of(generation.get()));
        when(taskMapper.toGenerationResponse(any())).thenAnswer(invocation -> response(generation.get()));
        when(similarityService.findSimilarForGeneration("prompt")).thenReturn(List.of(similar));
        var requested = new AtomicReference<TaskGenerationRequestedEvent>();
        when(taskGenerationEvents.send(any(), any(), any())).thenAnswer(invocation -> {
            requested.set(invocation.getArgument(2));
            return CompletableFuture.completedFuture(null);
        });

        generationService.generate(new TaskGenerationRequest("prompt"));
        generationService.processGeneration(requested.get());

        assertThat(generation.get().getStatus()).isEqualTo(TaskGenerationStatus.SIMILAR_TASKS_FOUND);
        verify(aiProvider, never()).generate(any(), any(), any(Double.class));
    }

    @Test
    void cancelGeneration_inProgress_marksGenerationCanceled() {
        var generation = generation();
        when(taskGenerationRepository.findById(generation.getId())).thenReturn(Optional.of(generation));

        generationService.cancelGeneration(generation.getId());

        assertThat(generation.getStatus()).isEqualTo(TaskGenerationStatus.CANCELED);
        assertThat(generation.getErrorMessage()).isEqualTo("Task generation canceled by user");
        verify(taskGenerationRepository).save(generation);
    }

    private TaskGenerationEntity generation() {
        return TaskGenerationEntity.builder()
                .id(UUID.randomUUID())
                .prompt("prompt")
                .status(TaskGenerationStatus.GENERATING)
                .attempt(0)
                .maxAttempts(3)
                .similarTasks(new ObjectMapper().createArrayNode())
                .build();
    }

    private TaskGenerationResponse response(TaskGenerationEntity generation) {
        return new TaskGenerationResponse(
                generation.getId(), generation.getPrompt(), generation.getStatus(),
                generation.getAttempt(), generation.getMaxAttempts(), null, List.of(),
                generation.getErrorMessage());
    }
}
