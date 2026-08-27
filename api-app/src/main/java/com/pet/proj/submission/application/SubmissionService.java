package com.pet.proj.submission.application;

import com.pet.proj.contracts.SubmissionCompletedEvent;
import com.pet.proj.contracts.SubmissionCreatedEvent;
import com.pet.proj.submission.domain.Submission;
import com.pet.proj.submission.domain.SubmissionStatus;
import com.pet.proj.submission.persistence.SubmissionEntity;
import com.pet.proj.submission.persistence.SubmissionRepository;
import com.pet.proj.user.application.UserAccountService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionService {
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    private final SubmissionRepository submissions;
    private final SubmissionMapper submissionMapper;
    private final KafkaTemplate<String, SubmissionCreatedEvent> submissionEvents;
    private final UserAccountService userAccounts;

    @Value("${app.kafka.topics.submission-created}")
    private String submissionCreatedTopic;
    private ConcurrentHashMap<UUID, CompletableFuture<ReferenceExecutionResult>> referenceExecutions = new ConcurrentHashMap<>();

    public Submission submit(UUID taskId, String sourceCode) {
        var userId = userAccounts.currentId();
        var saved = submissions.save(SubmissionEntity.builder()
                .taskId(taskId)
                .userId(userId)
                .sourceCode(sourceCode)
                .executionMode(SubmissionCreatedEvent.STANDARD)
                .status(SubmissionStatus.QUEUED)
                .build());
        var correlationId = UUID.randomUUID();

        submissionEvents.send(
                submissionCreatedTopic,
                correlationId.toString(),
                new SubmissionCreatedEvent(correlationId, saved.getId(), taskId)
        ).whenComplete((_, error) -> {
            if (error != null) {
                markQueueFailure(saved.getId(), error);
            }
        });
        return submissionMapper.toDomain(saved);
    }

    public Submission get(UUID id) {
        return submissionMapper.toDomain(getSubmissionEntity(id));
    }

    public List<Submission> validateReferenceSolutions(UUID taskId, List<String> referenceSolutions) {
        return referenceSolutions.stream()
                .map(sourceCode -> {
                    var result = executeReferenceSolution(taskId, sourceCode);
                    var status = result.passed() ? SubmissionStatus.PASSED : SubmissionStatus.FAILED;
                    return new Submission(null, taskId, sourceCode, status, 0, 0, result.error());
                })
                .toList();
    }

    public ReferenceExecutionResult executeReferenceSolution(UUID taskId, String sourceCode) {
        var saved = submissions.save(SubmissionEntity.builder()
                .taskId(taskId)
                .sourceCode(sourceCode)
                .executionMode(SubmissionCreatedEvent.REFERENCE_ORACLE)
                .status(SubmissionStatus.QUEUED)
                .build());
        var correlationId = UUID.randomUUID();
        var response = new CompletableFuture<ReferenceExecutionResult>();
        referenceExecutions.put(correlationId, response);

        submissionEvents.send(
                submissionCreatedTopic,
                correlationId.toString(),
                new SubmissionCreatedEvent(correlationId, saved.getId(), taskId,
                        SubmissionCreatedEvent.REFERENCE_ORACLE)
        ).whenComplete((_, error) -> {
            if (error != null) {
                markQueueFailure(saved.getId(), error);
                referenceExecutions.computeIfPresent(correlationId, (_, pending) -> {
                    pending.complete(new ReferenceExecutionResult(
                            false, List.of(), "reference solution could not be queued"));
                    return pending;
                });
            }
        });
        log.info("Reference oracle queued, taskId={}, submissionId={}, correlationId={}",
                taskId, saved.getId(), correlationId);
        return awaitReferenceExecution(correlationId, response);
    }

    @KafkaListener(topics = "${app.kafka.topics.submission-completed}", groupId = "algocoach-api")
    public void receive(SubmissionCompletedEvent event) {
        if (!SubmissionCreatedEvent.REFERENCE_ORACLE.equals(event.executionMode())) {
            return;
        }
        referenceExecutions.computeIfPresent(event.correlationId(), (_, response) -> {
            response.complete(new ReferenceExecutionResult(
                    SubmissionStatus.PASSED.name().equals(event.status()),
                    event.outputs(), event.error()));
            return response;
        });
    }

    private SubmissionEntity getSubmissionEntity(UUID id) {
        return submissions.findById(id)
                .orElseThrow(() -> new NoSuchElementException("submission not found"));
    }

    private void markQueueFailure(UUID submissionId, Throwable error) {
        submissions.findById(submissionId).ifPresent(submission -> {
            if (submission.getStatus() == SubmissionStatus.QUEUED) {
                submission.setStatus(SubmissionStatus.FAILED);
                submission.setError("submission could not be queued: " + error.getMessage());
                submissions.save(submission);
            }
        });
    }

    private ReferenceExecutionResult awaitReferenceExecution(UUID id, CompletableFuture<ReferenceExecutionResult> response) {
        try {
            return response.orTimeout(RESPONSE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                    .whenComplete((_, _) -> referenceExecutions.remove(id))
                    .join();
        } catch (Exception e) {
            referenceExecutions.remove(id);
            return new ReferenceExecutionResult(false, List.of(), "reference execution timed out");
        }
    }

    public record ReferenceExecutionResult(boolean passed, List<String> outputs, String error) {
    }
}
