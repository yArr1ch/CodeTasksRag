package com.pet.proj.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.contracts.SubmissionCreatedEvent;
import com.pet.proj.contracts.SubmissionCompletedEvent;
import com.pet.proj.submission.domain.SubmissionStatus;
import com.pet.proj.worker.execution.CodeExecutor;
import com.pet.proj.submission.persistence.SubmissionRepository;
import com.pet.proj.task.persistence.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionWorker {
    private final SubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final ObjectMapper json;
    private final CodeExecutor executor;
    private final KafkaTemplate<String, SubmissionCompletedEvent> completedEvents;

    @Value("${app.kafka.topics.submission-completed}")
    private String submissionCompletedTopic;

    @KafkaListener(topics = "${app.kafka.topics.submission-created}")
    public void process(SubmissionCreatedEvent event) {
        log.info(
                "Processing submission, taskId={}, submissionId={}, mode={}, correlationId={}",
                event.taskId(), event.submissionId(), event.executionMode(), event.correlationId());
        var submission = submissionRepository.findById(event.submissionId()).orElseThrow();
        if (submission.getStatus() != SubmissionStatus.QUEUED) {
            log.info("Skipping already processed submissionId={}, status={}",
                    submission.getId(), submission.getStatus());
            return;
        }

        if (submissionRepository.claimForExecution(
                submission.getId(), SubmissionStatus.QUEUED, SubmissionStatus.RUNNING) == 0) {
            log.info("Submission was claimed by another worker, submissionId={}", submission.getId());
            return;
        }
        submission.setStatus(SubmissionStatus.RUNNING);

        CodeExecutor.ExecutionResult result;
        try {
            var task = taskRepository.findById(event.taskId()).orElseThrow();
            var testCases = json.convertValue(task.getTestCases(), new TypeReference<List<CodeExecutor.TestCase>>() {
            });
            result = executor.execute(new CodeExecutor.ExecutionRequest(
                    submission.getSourceCode(), testCases,
                    SubmissionCreatedEvent.REFERENCE_ORACLE.equals(event.executionMode())));
        } catch (Exception exception) {
            log.error("Submission execution failed, submissionId={}", submission.getId(), exception);
            result = new CodeExecutor.ExecutionResult(
                    SubmissionStatus.FAILED,
                    0,
                    0,
                    exception.getMessage() == null ? "submission execution failed" : exception.getMessage(),
                    List.of());
        }

        submission.setStatus(result.status());
        submission.setPassedTests(result.passedTests());
        submission.setTotalTests(result.totalTests());
        submission.setError(result.error());
        submissionRepository.save(submission);

        completedEvents.send(submissionCompletedTopic, event.correlationId().toString(),
                new SubmissionCompletedEvent(event.correlationId(), event.submissionId(), event.taskId(),
                        result.status().name(), result.passedTests(), result.totalTests(), result.error(),
                        event.executionMode(), result.outputs()));
    }
}
