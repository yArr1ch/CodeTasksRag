package com.pet.proj.task.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.ai.AiProvider;
import com.pet.proj.coaching.application.KnowledgeDocumentService;
import com.pet.proj.submission.application.SubmissionService;
import com.pet.proj.task.api.TaskGenerationRequest;
import com.pet.proj.task.api.TaskGenerationResponse;
import com.pet.proj.task.api.TaskGenerationResult;
import com.pet.proj.task.domain.Task;
import com.pet.proj.task.domain.TaskGenerationStatus;
import com.pet.proj.task.domain.TaskStatus;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskGenerationEntity;
import com.pet.proj.task.persistence.TaskGenerationRepository;
import com.pet.proj.task.persistence.TaskRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskGenerationService {
    private final TaskRepository taskRepository;
    private final TaskGenerationRepository taskGenerationRepository;
    private final ObjectMapper objectMapper;
    private final AiProvider aiProvider;
    private final TaskSimilarityService taskSimilarityService;
    private final TaskMapper taskMapper;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final SubmissionService submissionService;
    private final ExecutorService aiExecutor;
    private final Semaphore limit;

    private final Map<UUID, Future<?>> generationJobs = new ConcurrentHashMap<>();

    @Value("${app.ai.max-concurrent-requests:3}")
    private int maxConcurrentRequests;

    @Value("${app.ai.deterministic-temperature:0.0}")
    private double deterministicTemperature;

    @Value("${app.ai.repair-temperature:0.25}")
    private double repairTemperature;

    @Value("${app.task-generation.max-attempts:3}")
    private int maxGenerationAttempts;

    @Value("${app.task-generation.knowledge-results:2}")
    private int knowledgeResults;

    @Value("${app.task-generation.timeout:10m}")
    private Duration generationTimeout;

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

        submitGeneration(generationId, request);
        return taskMapper.toGenerationResponse(generation);
    }

    private void submitGeneration(UUID generationId, TaskGenerationRequest request) {
        var job = new FutureTask<Void>(() -> {
            processGeneration(generationId, request);
            return null;
        });
        generationJobs.put(generationId, job);
        try {
            aiExecutor.execute(job);
            CompletableFuture.delayedExecutor(generationTimeout.toMillis(), TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        if (generationJobs.get(generationId) == job && !job.isDone()) {
                            failGeneration(generationId,
                                    "AI task generation timed out after " + generationTimeout.toMinutes() + " minutes");
                            job.cancel(true);
                            generationJobs.remove(generationId, job);
                        }
                    });
        } catch (RejectedExecutionException failure) {
            generationJobs.remove(generationId, job);
            failGeneration(generationId, "task generation could not be scheduled");
        }
    }

    private void processGeneration(UUID generationId, TaskGenerationRequest request) {
        try {
            log.info("Task generation started, generationId={}", generationId);
            var result = checkSimilarityAndGenerate(generationId, request.prompt());
            var generation = getGenerationEntity(generationId);
            if (!generation.isInProgress()) {
                return;
            }
            if (result.status() == TaskGenerationResult.GenerationStatus.SIMILAR_TASKS_FOUND) {
                generation.markSimilarTasks(objectMapper.valueToTree(result.similarTasks()));
            } else {
                generation.markReady(result.task().id());
            }
            taskGenerationRepository.save(generation);
            log.info("Task generation completed, generationId={}, status={}", generationId, generation.getStatus());
        } catch (CancellationException failure) {
            log.info("Task generation canceled, generationId={}", generationId);
        } catch (Exception failure) {
            failGeneration(generationId, Objects.toString(failure.getMessage(), "task generation failed"));
            log.error("Task generation failed, generationId={}", generationId, failure);
        } finally {
            generationJobs.remove(generationId);
        }
    }

    private TaskGenerationResult checkSimilarityAndGenerate(UUID generationId, String prompt) {
        ensureGenerationActive(generationId);
        updateGeneration(generationId, TaskGenerationStatus.GENERATING, 0);
        var knowledgeContext = knowledgeDocumentService.retrieveContext(prompt, knowledgeResults);
        log.info("Knowledge lookup completed, contextFound={}", StringUtils.hasText(knowledgeContext));

        ensureGenerationActive(generationId);
        var similarTasks = taskSimilarityService.findSimilarForGeneration(prompt);
        log.info("Task similarity check completed, matches={}", similarTasks.size());
        if (!similarTasks.isEmpty()) {
            log.info("Task generation stopped because similar tasks were found, generationId={}", generationId);
            return new TaskGenerationResult(
                    TaskGenerationResult.GenerationStatus.SIMILAR_TASKS_FOUND,
                    null,
                    similarTasks,
                    List.of());
        }
        return generateTask(generationId, prompt, knowledgeContext);
    }

    private TaskGenerationResult generateTask(UUID generationId, String requestPrompt, String knowledgeContext) {
        var basePrompt = new TaskGenerationPrompt(requestPrompt, knowledgeContext).render();
        var lastFailure = new AtomicReference<ReferenceSolutionException>();
        var lastDraft = new AtomicReference<GeneratedTaskDraft>();

        return IntStream.rangeClosed(1, maxGenerationAttempts)
                .mapToObj(attempt -> {
                    ensureGenerationActive(generationId);
                    GeneratedTaskDraft draft;
                    if (attempt == 1) {
                        updateGeneration(generationId, TaskGenerationStatus.GENERATING, attempt);
                        log.info("Calling AI for task generation, attempt={}/{}", attempt, maxGenerationAttempts);
                        draft = askForPermission(() -> aiProvider.generate(
                                basePrompt, GeneratedTaskDraft.class, deterministicTemperature));
                    } else {
                        var previousDraft = Objects.requireNonNull(lastDraft.get(), "previous task draft is missing");
                        var failure = Objects.requireNonNull(lastFailure.get(), "previous repair failure is missing");
                        updateGeneration(generationId, TaskGenerationStatus.REPAIRING, attempt);
                        var prompt = new TaskRepairPrompt(
                                previousDraft.title(),
                                previousDraft.description(),
                                previousDraft.constraints(),
                                failure,
                                attempt).render();
                        log.info("Calling AI for reference repair, attempt={}/{}", attempt, maxGenerationAttempts);
                        var repair = askForPermission(() -> aiProvider.generate(
                                prompt, ReferenceCodeRepair.class, repairTemperature));
                        draft = new GeneratedTaskDraft(
                                previousDraft.title(),
                                previousDraft.description(),
                                previousDraft.constraints(),
                                previousDraft.testCases(),
                                previousDraft.concepts(),
                                repair.sourceCodeLines());
                    }
                    lastDraft.set(draft);
                    try {
                        updateGeneration(generationId, TaskGenerationStatus.VALIDATING, attempt);
                        return persistValidatedDraft(generationId, requestPrompt, draft);
                    } catch (ReferenceSolutionException failure) {
                        lastFailure.set(failure);
                        log.warn("Reference solution rejected, attempt={}/{}: {}",
                                attempt, maxGenerationAttempts, failure.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> {
                    var failure = lastFailure.get();
                    var message = failure == null
                            ? "task generation failed"
                            : Objects.toString(failure.getMessage(), "task generation failed");
                    return new IllegalArgumentException(
                            "could not generate a valid reference solution after " + maxGenerationAttempts
                                    + " attempts: " + message,
                            failure);
                });
    }

    public TaskGenerationResponse getGeneration(UUID id) {
        return taskMapper.toGenerationResponse(getGenerationEntity(id));
    }

    public void cancelGeneration(UUID id) {
        var generation = getGenerationEntity(id);
        if (!generation.isInProgress()) {
            throw new IllegalStateException("only in-progress task generations can be canceled");
        }
        generation.markCanceled();
        taskGenerationRepository.save(generation);
        var job = generationJobs.remove(id);
        if (job != null) {
            job.cancel(true);
        }
    }

    private <T> T askForPermission(Supplier<T> action) {
        var acquired = false;
        try {
            limit.acquire();
            acquired = true;
            return action.get();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new CancellationException("task generation interrupted");
        } finally {
            if (acquired) {
                limit.release();
            }
        }
    }

    private void updateGeneration(UUID id, TaskGenerationStatus status, int attempt) {
        var generation = getGenerationEntity(id);
        if (!generation.isInProgress()) {
            throw new CancellationException("task generation is no longer active");
        }
        generation.markProgress(status, attempt);
        taskGenerationRepository.save(generation);
    }

    private void ensureGenerationActive(UUID id) {
        if (!getGenerationEntity(id).isInProgress()) {
            throw new CancellationException("task generation is no longer active");
        }
    }

    private void failGeneration(UUID id, String message) {
        taskGenerationRepository.findById(id).ifPresent(generation -> {
            if (generation.isInProgress()) {
                generation.markFailed(message);
                taskGenerationRepository.save(generation);
            }
        });
    }

    private TaskGenerationEntity getGenerationEntity(UUID id) {
        return taskGenerationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("generation not found: " + id));
    }

    private TaskGenerationResult persistValidatedDraft(UUID generationId, String requestPrompt, GeneratedTaskDraft draft) {
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
                    .constraints(objectMapper.valueToTree(draft.constraints()))
                    .generatedByAi(true)
                    .generationPrompt(requestPrompt)
                    .referenceSolutions(objectMapper.valueToTree(referenceSolutions))
                    .testCases(objectMapper.valueToTree(generatedTestCases))
                    .concepts(objectMapper.valueToTree(draft.concepts()))
                    .status(TaskStatus.DRAFT)
                    .build();

            ensureGenerationActive(generationId);
            var saved = taskRepository.saveAndFlush(entity);
            var oracleResult = submissionService.executeReferenceSolution(saved.getId(), referenceSolutions.getFirst());
            ensureGenerationActive(generationId);
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
            ensureGenerationActive(generationId);
            saved.setTestCases(objectMapper.valueToTree(finalizedTestCases));
            saved = taskRepository.saveAndFlush(saved);
            ensureGenerationActive(generationId);
            var task = taskMapper.toDomain(saved);
            taskSimilarityService.index(saved);

            return new TaskGenerationResult(
                    TaskGenerationResult.GenerationStatus.GENERATED,
                    task,
                    List.of(),
                    referenceSolutions);
        } catch (Exception failure) {
            if (entity != null) {
                taskRepository.delete(entity);
            }
            if (failure instanceof CancellationException cancellation) {
                throw cancellation;
            }
            if (failure instanceof ReferenceSolutionException referenceFailure) {
                throw referenceFailure;
            }
            throw new IllegalArgumentException(
                    "could not generate a validated task draft: "
                            + Objects.toString(failure.getMessage(), "task generation failed"),
                    failure);
        }
    }

    private record GeneratedTaskDraft(
            @NotBlank String title,
            @NotBlank String description,
            @NotEmpty List<String> constraints,
            @NotEmpty List<GeneratedTestCase> testCases,
            @NotEmpty List<String> concepts,
            @NotEmpty List<String> referenceSolutionLines
    ) {
        private record GeneratedTestCase(@NotBlank String input) {
        }
    }

    private record ReferenceCodeRepair(@NotEmpty List<String> sourceCodeLines) {
    }
}
