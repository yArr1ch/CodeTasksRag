package com.pet.proj.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.contracts.SubmissionCompletedEvent;
import com.pet.proj.contracts.SubmissionCreatedEvent;
import com.pet.proj.submission.domain.SubmissionStatus;
import com.pet.proj.submission.persistence.SubmissionEntity;
import com.pet.proj.submission.persistence.SubmissionRepository;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import com.pet.proj.worker.execution.CodeExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionWorkerTest {
    @Mock
    private SubmissionRepository submissions;

    @Mock
    private TaskRepository tasks;

    @Mock
    private CodeExecutor executor;

    @Mock
    private KafkaTemplate<String, SubmissionCompletedEvent> completedEvents;

    private SubmissionWorker worker;

    @BeforeEach
    void setUp() {
        worker = new SubmissionWorker(submissions, tasks, new ObjectMapper(), executor, completedEvents);
        ReflectionTestUtils.setField(worker, "submissionCompletedTopic", "submission-completed");
    }

    @Test
    void marksSubmissionRunningThenPersistsExecutionResult() {
        var taskId = UUID.randomUUID();
        var submissionId = UUID.randomUUID();
        var correlationId = UUID.randomUUID();
        var submission = SubmissionEntity.builder()
                .id(submissionId)
                .taskId(taskId)
                .sourceCode("class Main {}")
                .executionMode(SubmissionCreatedEvent.STANDARD)
                .status(SubmissionStatus.QUEUED)
                .build();
        var task = TaskEntity.builder()
                .id(taskId)
                .testCases(new ObjectMapper().createArrayNode())
                .build();
        when(submissions.findById(submissionId)).thenReturn(Optional.of(submission));
        when(submissions.claimForExecution(
                submissionId, SubmissionStatus.QUEUED, SubmissionStatus.RUNNING)).thenReturn(1);
        when(tasks.findById(taskId)).thenReturn(Optional.of(task));
        when(executor.execute(any())).thenReturn(new CodeExecutor.ExecutionResult(
                SubmissionStatus.PASSED, 1, 1, null, List.of()));

        worker.process(new SubmissionCreatedEvent(correlationId, submissionId, taskId));

        verify(submissions).claimForExecution(
                submissionId, SubmissionStatus.QUEUED, SubmissionStatus.RUNNING);
        verify(submissions).save(submission);
        verify(executor).execute(any());
        verify(completedEvents).send(any(), any(), any());
    }

    @Test
    void skipsAlreadyProcessedSubmission() {
        var submission = SubmissionEntity.builder()
                .id(UUID.randomUUID())
                .taskId(UUID.randomUUID())
                .sourceCode("class Main {}")
                .executionMode(SubmissionCreatedEvent.STANDARD)
                .status(SubmissionStatus.PASSED)
                .build();
        when(submissions.findById(submission.getId())).thenReturn(Optional.of(submission));

        worker.process(new SubmissionCreatedEvent(
                UUID.randomUUID(), submission.getId(), submission.getTaskId()));

        verify(executor, never()).execute(any());
        verify(tasks, never()).findById(any());
        verify(completedEvents, never()).send(any(), any(), any());
    }
}
