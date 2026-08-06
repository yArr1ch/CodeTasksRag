package com.pet.proj.task.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.ai.AiProvider;
import com.pet.proj.coaching.application.KnowledgeDocumentService;
import com.pet.proj.submission.application.SubmissionService;
import com.pet.proj.task.api.TaskCorrectionRequest;
import com.pet.proj.task.domain.Task;
import com.pet.proj.task.domain.TaskStatus;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private AiProvider aiProvider;
    @Mock
    private TaskSimilarityService similarityService;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private KnowledgeDocumentService knowledgeDocuments;
    @Mock
    private SubmissionService submissions;
    @Mock
    private ExecutorService aiExecutor;

    @InjectMocks
    private TaskService taskService;

    @Test
    void applyCorrection_draftDescription_updatesTask() {
        var taskId = UUID.randomUUID();
        var task = draftTask();
        var correctedTask = new Task(
                taskId, "Task", "Updated description", List.of(), List.of(), TaskStatus.DRAFT);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskMapper.toDomain(task)).thenReturn(correctedTask);

        var result = taskService.applyCorrection(
                taskId, new TaskCorrectionRequest("description", "Updated description"));

        assertThat(result).isEqualTo(correctedTask);
        assertThat(task.getDescription()).isEqualTo("Updated description");
        verify(taskMapper).toDomain(task);
    }

    @Test
    void applyCorrection_publishedTask_rejectsCorrection() {
        var taskId = UUID.randomUUID();
        var task = draftTask();
        task.setStatus(TaskStatus.PUBLISHED);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.applyCorrection(
                taskId, new TaskCorrectionRequest("description", "Updated description")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("only draft tasks can be corrected");
        verifyNoInteractions(taskMapper);
    }

    private TaskEntity draftTask() {
        var mapper = new ObjectMapper();
        return TaskEntity.builder()
                .title("Task")
                .description("Original description")
                .constraints(mapper.createArrayNode())
                .generatedByAi(true)
                .referenceSolutions(mapper.createArrayNode())
                .testCases(mapper.createArrayNode())
                .concepts(mapper.createArrayNode())
                .status(TaskStatus.DRAFT)
                .build();
    }
}
