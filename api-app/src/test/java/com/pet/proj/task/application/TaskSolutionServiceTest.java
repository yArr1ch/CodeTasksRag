package com.pet.proj.task.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.ai.SemanticSearchService;
import com.pet.proj.contracts.SubmissionCompletedEvent;
import com.pet.proj.contracts.SubmissionCreatedEvent;
import com.pet.proj.submission.domain.SubmissionStatus;
import com.pet.proj.submission.persistence.SubmissionRepository;
import com.pet.proj.task.domain.TaskStatus;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSolutionServiceTest {
    @Mock
    private TaskRepository tasks;
    @Mock
    private SubmissionRepository submissions;
    @Mock
    private SemanticSearchService semanticSearch;

    @Test
    void draftSolutionsAreReadFromDatabaseWithoutVectorWrites() {
        var service = new TaskSolutionService(tasks, submissions, semanticSearch);
        var task = task(TaskStatus.DRAFT);
        when(tasks.findById(task.getId())).thenReturn(Optional.of(task));

        var result = service.findSolutions(task.getId(), null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(com.pet.proj.task.api.TaskSolution.SolutionType.REFERENCE);
        verifyNoInteractions(semanticSearch);
    }

    @Test
    void passedStandardSubmissionReindexesPublishedTask() {
        var service = new TaskSolutionService(tasks, submissions, semanticSearch);
        var task = task(TaskStatus.PUBLISHED);
        when(tasks.findById(task.getId())).thenReturn(Optional.of(task));
        when(submissions.findByTaskIdAndStatusAndExecutionMode(
                task.getId(), SubmissionStatus.PASSED, SubmissionCreatedEvent.STANDARD))
                .thenReturn(List.of());

        service.handleSubmissionCompleted(new SubmissionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), task.getId(), SubmissionStatus.PASSED.name(),
                1, 1, null, SubmissionCreatedEvent.STANDARD, List.of("output")));

        verify(semanticSearch).replace(eq("documentType == 'solution' && taskId == '" + task.getId() + "'"), anyList());
    }

    @Test
    void publishedSolutionReadDoesNotReplaceVectorDocuments() {
        var service = new TaskSolutionService(tasks, submissions, semanticSearch);
        var task = task(TaskStatus.PUBLISHED);
        when(tasks.findById(task.getId())).thenReturn(Optional.of(task));
        when(semanticSearch.search(
                org.mockito.ArgumentMatchers.anyString(), eq(5), eq(0.0),
                eq("documentType == 'solution' && taskId == '" + task.getId() + "' && status == 'PASSED'")))
                .thenReturn(List.of());

        assertThat(service.findSolutions(task.getId(), null)).isEmpty();

        verify(semanticSearch).search(
                org.mockito.ArgumentMatchers.anyString(), eq(5), eq(0.0),
                eq("documentType == 'solution' && taskId == '" + task.getId() + "' && status == 'PASSED'"));
        org.mockito.Mockito.verify(semanticSearch, org.mockito.Mockito.never()).replace(
                org.mockito.ArgumentMatchers.anyString(), anyList());
    }

    @Test
    void publishedSolutionRead_duplicateVectorDocuments_returnsOneSolution() {
        var service = new TaskSolutionService(tasks, submissions, semanticSearch);
        var task = task(TaskStatus.PUBLISHED);
        var solutionId = UUID.randomUUID();
        var metadata = Map.of(
                "documentType", "solution",
                "taskId", task.getId().toString(),
                "solutionId", solutionId.toString(),
                "solutionType", "COMMUNITY",
                "status", "PASSED");
        when(tasks.findById(task.getId())).thenReturn(Optional.of(task));
        when(semanticSearch.search(
                org.mockito.ArgumentMatchers.anyString(), eq(5), eq(0.0),
                eq("documentType == 'solution' && taskId == '" + task.getId() + "' && status == 'PASSED'")))
                .thenReturn(List.of(
                        new Document(solutionId.toString(), "context\n\nfirst", metadata),
                        new Document(solutionId.toString(), "context\n\nsecond", metadata)));

        assertThat(service.findSolutions(task.getId(), null))
                .extracting(com.pet.proj.task.api.TaskSolution::id)
                .containsExactly(solutionId);
    }

    @Test
    void referenceOracleEventDoesNotUpdateCommunitySolutions() {
        var service = new TaskSolutionService(tasks, submissions, semanticSearch);

        service.handleSubmissionCompleted(new SubmissionCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SubmissionStatus.PASSED.name(),
                1, 1, null, SubmissionCreatedEvent.REFERENCE_ORACLE, List.of("output")));

        verifyNoInteractions(tasks, submissions, semanticSearch);
    }

    @Test
    void reindexRemovesStaleSolutionDocuments() {
        var service = new TaskSolutionService(tasks, submissions, semanticSearch);
        when(tasks.findAll()).thenReturn(List.of(task(TaskStatus.PUBLISHED), task(TaskStatus.REJECTED)));
        when(submissions.findByTaskIdAndStatusAndExecutionMode(
                org.mockito.ArgumentMatchers.any(), eq(SubmissionStatus.PASSED),
                eq(SubmissionCreatedEvent.STANDARD))).thenReturn(List.of());

        assertThat(service.reindex()).isEqualTo(1);
        verify(semanticSearch).replace(eq("documentType == 'solution'"), anyList());
    }

    private TaskEntity task(TaskStatus status) {
        var mapper = new ObjectMapper();
        return TaskEntity.builder()
                .id(UUID.randomUUID())
                .title("Task")
                .description("Description")
                .constraints(mapper.createArrayNode())
                .referenceSolutions(mapper.valueToTree(List.of("class Main {}")))
                .testCases(mapper.createArrayNode())
                .concepts(mapper.createArrayNode())
                .status(status)
                .build();
    }
}
