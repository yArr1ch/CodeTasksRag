package com.pet.proj.task.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.ai.AiProvider;
import com.pet.proj.coaching.application.KnowledgeDocumentService;
import com.pet.proj.submission.application.SubmissionService;
import com.pet.proj.submission.domain.Submission;
import com.pet.proj.submission.domain.SubmissionStatus;
import com.pet.proj.task.api.*;
import com.pet.proj.task.domain.Task;
import com.pet.proj.task.domain.TaskStatus;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private static final int MAX_GENERATION_ATTEMPTS = 3;

    private final TaskRepository taskRepository;
    private final ObjectMapper mapper;
    private final AiProvider aiProvider;
    private final TaskSimilarityService similarityService;
    private final TaskMapper taskMapper;
    private final KnowledgeDocumentService knowledgeDocuments;
    private final SubmissionService submissions;
    private final ExecutorService aiExecutor;

    private final Semaphore limit = new Semaphore(3);

    public int reindexTaskEmbeddings() {
        return similarityService.reindex();
    }

    public CompletableFuture<TaskGenerationResult> generate(TaskGenerationRequest request) {
        var generationId = UUID.randomUUID();
        log.info("Task generation requested, generationId={}, decision={}", generationId, request.decision());

        return CompletableFuture
                .supplyAsync(() -> {
                    log.info("Task generation started, generationId={}", generationId);
                    return request.decision() == TaskGenerationRequest.GenerationDecision.CHECK
                            ? checkSimilarityAndGenerate(request.prompt())
                            : generateTask(request.prompt());
                }, aiExecutor)
                .whenComplete((result, error) -> {
                    if (error == null) {
                        log.info("Task generation completed, generationId={}, status={}", generationId, result.status());
                    } else {
                        log.error("Task generation failed, generationId={}", generationId, error);
                    }
                })
                .orTimeout(10, TimeUnit.MINUTES);
    }

    private <T> T askForPermission(Supplier<T> action) {
        var toRelease = false;
        try {
            limit.acquire();
            toRelease = true;
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI request interrupted", e);
        } finally {
            if (toRelease) {
                limit.release();
            }
        }
    }

    private TaskGenerationResult checkSimilarityAndGenerate(String prompt) {
        var similarTasks = similarityService.findSimilar(prompt);
        log.info("Task similarity check completed, matches={}", similarTasks.size());
        if (!similarTasks.isEmpty()) {
            log.info("Task generation stopped because similar tasks were found");
            return new TaskGenerationResult(
                    TaskGenerationResult.GenerationStatus.SIMILAR_TASKS_FOUND,
                    null,
                    similarTasks,
                    List.of());
        }
        return generateTask(prompt);
    }

    private TaskGenerationResult generateTask(String requestPrompt) {
        log.info("Retrieving knowledge context for task generation");

        var knowledgeContext = knowledgeDocuments.retrieveContext(requestPrompt, 2);
        var basePrompt = new TaskGenerationPrompt(requestPrompt, knowledgeContext).render();
        var lastFailure = new AtomicReference<ReferenceSolutionException>();

        return IntStream.rangeClosed(1, MAX_GENERATION_ATTEMPTS)
                .mapToObj(attempt -> {
                    var prompt = lastFailure.get() == null
                            ? basePrompt
                            : new TaskRepairPrompt(basePrompt, lastFailure.get(), attempt).render();
                    log.info("Calling AI for task generation, attempt={}/{}", attempt, MAX_GENERATION_ATTEMPTS);
                    var draft = askForPermission(() -> aiProvider.generate(prompt, GeneratedTaskDraft.class));
                    try {
                        return persistValidatedDraft(requestPrompt, draft);
                    } catch (ReferenceSolutionException failure) {
                        lastFailure.set(failure);
                        log.warn("Reference solution rejected, attempt={}/{}: {}",
                                attempt, MAX_GENERATION_ATTEMPTS, failure.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "could not generate a valid reference solution after " + MAX_GENERATION_ATTEMPTS
                                + " attempts: " + lastFailure.get().getMessage(),
                        lastFailure.get()));
    }

    private TaskGenerationResult persistValidatedDraft(String requestPrompt, GeneratedTaskDraft draft) {
        var referenceSolutions = List.of(String.join("\n", draft.referenceSolutionLines()));
        if (CollectionUtils.isEmpty(referenceSolutions)
                || referenceSolutions.stream().anyMatch(solution -> !StringUtils.hasText(solution))) {
            throw new IllegalArgumentException("AI must return at least one complete reference solution");
        }
        TaskEntity entity = null;
        try {
            var generatedTestCases = draft.testCases().stream()
                    .map(testCase -> new Task.TestCase(testCase.input(), null))
                    .toList();
            entity = TaskEntity.builder()
                    .title(draft.title())
                    .description(draft.description())
                    .constraints(mapper.valueToTree(draft.constraints()))
                    .generatedByAi(true)
                    .generationPrompt(requestPrompt)
                    .referenceSolutions(mapper.valueToTree(referenceSolutions))
                    .testCases(mapper.valueToTree(generatedTestCases))
                    .concepts(mapper.valueToTree(draft.concepts()))
                    .status(TaskStatus.DRAFT)
                    .build();

            var saved = taskRepository.saveAndFlush(entity);
            var oracleResult = submissions.executeReferenceSolution(saved.getId(), referenceSolutions.getFirst());
            if (!oracleResult.passed()) {
                throw new ReferenceSolutionException(oracleResult.error(), referenceSolutions.getFirst());
            }
            var expectedOutputs = oracleResult.outputs();
            if (expectedOutputs.size() != generatedTestCases.size()) {
                throw new IllegalStateException("reference solution returned an unexpected number of outputs");
            }
            var finalizedTestCases = IntStream.range(0, generatedTestCases.size())
                    .mapToObj(index -> new Task.TestCase(
                            generatedTestCases.get(index).input(), expectedOutputs.get(index)))
                    .toList();
            saved.setTestCases(mapper.valueToTree(finalizedTestCases));
            saved = taskRepository.saveAndFlush(saved);
            similarityService.index(saved);
            var task = taskMapper.toDomain(saved);

            return new TaskGenerationResult(
                    TaskGenerationResult.GenerationStatus.GENERATED,
                    task,
                    similarityService.findSimilar(saved),
                    referenceSolutions);
        } catch (Exception e) {
            if (entity != null) {
                taskRepository.delete(entity);
            }
            if (e instanceof ReferenceSolutionException referenceFailure) {
                throw referenceFailure;
            }
            throw new IllegalArgumentException(
                    "could not generate a validated task draft: " + e.getCause().getMessage(), e);
        }
    }

    public CompletableFuture<TaskReview> review(UUID id) {
        log.info("Task review requested, taskId={}", id);

        return CompletableFuture.supplyAsync(() -> {
                    var entity = getTaskEntity(id);
                    var referenceSolutions = mapper.convertValue(
                            entity.getReferenceSolutions(),
                            new TypeReference<List<String>>() {
                            });
                    var executionResults = submissions.validateReferenceSolutions(id, referenceSolutions);
                    var executionFailure = executionResults.stream()
                            .filter(result -> result.status() != SubmissionStatus.PASSED)
                            .findFirst();
                    if (executionFailure.isPresent()) {
                        return referenceValidationReview(executionFailure.get());
                    }
                    var prompt = taskMapper.toReviewPrompt(entity).render();
                    return askForPermission(
                            () -> aiProvider.generate(prompt, TaskReview.class));
                }, aiExecutor)
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
                "Fix the reference solution or align the test input format with its parser.",
                null)));
    }

    @Transactional
    public Task applyCorrection(UUID id, TaskCorrectionRequest request) {
        var entity = getTaskEntity(id);
        if (entity.getStatus() != TaskStatus.DRAFT) {
            throw new IllegalStateException("only draft tasks can be corrected");
        }
        try {
            var value = request.correctedValue();
            switch (request.field()) {
                case "title" -> entity.setTitle(value);
                case "description" -> entity.setDescription(value);
                case "constraints" -> entity.setConstraints(mapper.readTree(value));
                case "testCases" -> entity.setTestCases(mapper.readTree(value));
                case "concepts" -> entity.setConcepts(mapper.readTree(value));
                case "referenceSolutions" -> entity.setReferenceSolutions(value.trim().startsWith("[")
                        ? mapper.readTree(value) : mapper.valueToTree(List.of(value)));
                default -> throw new IllegalArgumentException("unsupported correction field: " + request.field());
            }
            return taskMapper.toDomain(entity);
        } catch (Exception exception) {
            throw new IllegalArgumentException("corrected value is not valid for field " + request.field(), exception);
        }
    }

    @Transactional(readOnly = true)
    public List<Task> getPublishedTasks() {
        return taskRepository.findByStatus(TaskStatus.PUBLISHED)
                .stream()
                .map(taskMapper::toDomain)
                .toList();
    }

    public Task getTask(UUID id) {
        return taskMapper.toDomain(getTaskEntity(id));
    }

    @Transactional
    public void publish(UUID id) {
        updateStatus(id, TaskStatus.PUBLISHED);
    }

    @Transactional
    public void reject(UUID id) {
        updateStatus(id, TaskStatus.REJECTED);
    }

    private void updateStatus(UUID id, TaskStatus status) {
        var entity = getTaskEntity(id);
        entity.setStatus(status);
    }

    private TaskEntity getTaskEntity(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("task not found"));
    }

    public CompletableFuture<TaskHintResponse> generateHint(UUID id, TaskHintRequest request) {
        return CompletableFuture.supplyAsync(() -> {
                    var task = getTaskEntity(id);
                    var query = String.join("\n",
                            task.getTitle(),
                            task.getDescription(),
                            task.getConcepts().toString(),
                            "hint level " + request.level());

                    var knowledgeContext = knowledgeDocuments.retrieveContext(query, 3);
                    var prompt = taskMapper.toHintPrompt(task, request.level(), knowledgeContext).render();
                    return askForPermission(
                            () -> aiProvider.generate(prompt, HintDraft.class));
                }, aiExecutor)
                .thenApply(draft ->
                        new TaskHintResponse(request.level(), draft.hint()))
                .whenComplete((_, error) -> {
                    if (error == null) {
                        log.info("Task review completed, taskId={}", id);
                    } else {
                        log.error("Task review failed, taskId={}", id, error);
                    }
                });
    }

    private record HintDraft(
            @NotNull String hint) {
    }

    private record GeneratedTaskDraft(
            @NotBlank String title,
            @NotBlank String description,
            @NotEmpty List<String> constraints,
            @NotEmpty List<GeneratedTestCase> testCases,
            @NotEmpty List<String> concepts,
            @NotEmpty List<String> referenceSolutionLines
    ) {
    }

    private record GeneratedTestCase(
            @NotBlank String input) {
    }
}
