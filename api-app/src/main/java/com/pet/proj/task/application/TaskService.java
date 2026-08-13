package com.pet.proj.task.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.ai.AiProvider;
import com.pet.proj.coaching.application.KnowledgeDocumentService;
import com.pet.proj.submission.application.SubmissionService;
import com.pet.proj.submission.domain.Submission;
import com.pet.proj.submission.domain.SubmissionStatus;
import com.pet.proj.task.api.TaskHintRequest;
import com.pet.proj.task.api.TaskHintResponse;
import com.pet.proj.task.api.TaskPageResponse;
import com.pet.proj.task.api.TaskReview;
import com.pet.proj.task.domain.Task;
import com.pet.proj.task.domain.TaskStatus;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;
    private final AiProvider aiProvider;
    private final TaskSimilarityService similarityService;
    private final TaskMapper taskMapper;
    private final TaskSolutionService taskSolutionService;
    private final KnowledgeDocumentService knowledgeDocuments;
    private final SubmissionService submissionService;
    private final ExecutorService aiExecutor;
    private final Semaphore limit;

    @Value("${app.ai.max-concurrent-requests:3}")
    private int maxConcurrentRequests;

    @Value("${app.ai.deterministic-temperature:0.0}")
    private double aiTemperature;

    @Value("${app.task-hint.knowledge-results:3}")
    private int hintKnowledgeResults;

    @Value("${app.task-generation.timeout:10m}")
    private Duration aiTimeout;

    public int reindexTaskEmbeddings() {
        return similarityService.reindex();
    }

    public CompletableFuture<TaskReview> review(UUID id) {
        log.info("Task review requested, taskId={}", id);
        return CompletableFuture.supplyAsync(() -> {
                    var entity = getTaskEntity(id);

                    var referenceSolutions = objectMapper.convertValue(
                            entity.getReferenceSolutions(),
                            new TypeReference<List<String>>() {
                            });

                    var executionResults = submissionService.validateReferenceSolutions(id, referenceSolutions);
                    var executionFailure = executionResults.stream()
                            .filter(result -> result.status() != SubmissionStatus.PASSED)
                            .findFirst();
                    if (executionFailure.isPresent()) {
                        return referenceValidationReview(executionFailure.get());
                    }
                    var prompt = taskMapper.toReviewPrompt(entity).render();
                    return askForPermission(() -> aiProvider.generate(
                            prompt, TaskReview.class, aiTemperature));
                }, aiExecutor)
                .orTimeout(aiTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(failure -> {
                    if (failure.getCause() instanceof TimeoutException) {
                        throw new CompletionException(new IllegalStateException(
                                timeoutMessage("task review"), failure));
                    }
                    throw new CompletionException(failure);
                })
                .whenComplete((_, error) -> {
                    if (error == null) {
                        log.info("Task review completed, taskId={}", id);
                    } else {
                        log.error("Task review failed, taskId={}", id, error);
                    }
                });
    }

    private TaskReview referenceValidationReview(Submission submission) {
        return new TaskReview(false, List.of(new TaskReview.FieldWarning(
                "referenceSolutions",
                TaskReview.FieldWarning.Severity.ERROR,
                "Reference solution failed deterministic execution",
                submission.error(),
                "The task cannot be judged reliably until its reference solution passes every test.",
                "Fix the reference solution or align the test input format with its parser.")));
    }

    public CompletableFuture<TaskHintResponse> generateHint(UUID id, TaskHintRequest request) {
        return CompletableFuture.supplyAsync(() -> {
                    var task = getTaskEntity(id);
                    var query = String.join("\n",
                            task.getTitle(),
                            task.getDescription(),
                            task.getConcepts().toString(),
                            request.executionFeedback());
                    var knowledgeContext = knowledgeDocuments.retrieveContext(query, hintKnowledgeResults);
                    var prompt = taskMapper.toHintPrompt(
                            task, request.code(), request.executionFeedback(), knowledgeContext).render();
                    return askForPermission(() -> aiProvider.generate(
                            prompt, HintDraft.class, aiTemperature));
                }, aiExecutor)
                .orTimeout(aiTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(failure -> {
                    if (failure.getCause() instanceof TimeoutException) {
                        throw new CompletionException(new IllegalStateException(
                                timeoutMessage("task hint"), failure));
                    }
                    throw new CompletionException(failure);
                })
                .thenApply(draft -> new TaskHintResponse(draft.hint()))
                .whenComplete((_, error) -> {
                    if (error == null) {
                        log.info("Task hint completed, taskId={}", id);
                    } else {
                        log.error("Task hint failed, taskId={}", id, error);
                    }
                });
    }

    private <T> T askForPermission(Supplier<T> action) {
        var acquired = false;
        try {
            limit.acquire();
            acquired = true;
            return action.get();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI request interrupted", failure);
        } finally {
            if (acquired) {
                limit.release();
            }
        }
    }

    private String timeoutMessage(String operation) {
        return "AI " + operation + " timed out after " + aiTimeout.toMinutes() + " minutes";
    }

    public TaskPageResponse getPublishedTasks(int limit, UUID cursor) {
        var pageSize = limit + 1;
        var entities = cursor == null
                ? taskRepository.findByStatusOrderByIdAsc(TaskStatus.PUBLISHED, PageRequest.of(0, pageSize))
                : taskRepository.findByStatusAndIdGreaterThanOrderByIdAsc(
                TaskStatus.PUBLISHED, cursor, PageRequest.of(0, pageSize));
        var hasMore = entities.size() > limit;
        var tasks = entities.stream()
                .limit(limit)
                .map(taskMapper::toDomain)
                .toList();
        var nextCursor = hasMore ? tasks.getLast().id() : null;
        return new TaskPageResponse(tasks, nextCursor, hasMore);
    }

    public Task getTask(UUID id) {
        return taskMapper.toDomain(getTaskEntity(id));
    }

    @Transactional
    public void publish(UUID id) {
        var entity = getTaskEntity(id);
        entity.publish();
        similarityService.index(entity);
        taskSolutionService.indexPublishedTask(entity);
    }

    @Transactional
    public void reject(UUID id) {
        var entity = getTaskEntity(id);
        entity.reject();
        similarityService.remove(id);
        taskSolutionService.remove(id);
    }

    private TaskEntity getTaskEntity(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("task not found"));
    }

    private record HintDraft(@NotNull String hint) {
    }
}
