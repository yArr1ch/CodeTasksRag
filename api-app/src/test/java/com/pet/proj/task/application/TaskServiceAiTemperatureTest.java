package com.pet.proj.task.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.ai.OllamaProvider;
import com.pet.proj.coaching.application.KnowledgeDocumentService;
import com.pet.proj.submission.application.SubmissionService;
import com.pet.proj.task.api.TaskReview;
import com.pet.proj.task.domain.TaskStatus;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceAiTemperatureTest {
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private OllamaProvider aiProvider;
    @Mock
    private TaskSimilarityService similarityService;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskSolutionService solutions;
    @Mock
    private KnowledgeDocumentService knowledgeDocuments;
    @Mock
    private SubmissionService submissions;

    @Test
    void review_usesDeterministicTemperature() throws Exception {
        var taskId = UUID.randomUUID();
        var task = task();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(submissions.validateReferenceSolutions(taskId, List.of("class Main {}"))).thenReturn(List.of());
        when(taskMapper.toReviewPrompt(task))
                .thenReturn(new TaskReviewPrompt("title", "description", "constraints", "tests", "concepts"));
        when(aiProvider.generate(anyString(), eq(TaskReview.class), eq(0.0)))
                .thenReturn(new TaskReview(true, List.of()));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var service = new TaskService(
                    taskRepository,
                    new ObjectMapper(),
                    aiProvider,
                    similarityService,
                    taskMapper,
                    solutions,
                    knowledgeDocuments,
                    submissions,
                    executor);

            var result = service.review(taskId).get();

            assertThat(result.valid()).isTrue();
            verify(aiProvider).generate(anyString(), eq(TaskReview.class), eq(0.0));
        }
    }

    private TaskEntity task() {
        var mapper = new ObjectMapper();
        return TaskEntity.builder()
                .id(UUID.randomUUID())
                .title("title")
                .description("description")
                .constraints(mapper.createArrayNode())
                .referenceSolutions(mapper.valueToTree(List.of("class Main {}")))
                .testCases(mapper.createArrayNode())
                .concepts(mapper.createArrayNode())
                .status(TaskStatus.DRAFT)
                .build();
    }
}
