package com.pet.proj.task.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.ai.OllamaProvider;
import com.pet.proj.coaching.application.KnowledgeDocumentService;
import com.pet.proj.submission.application.SubmissionService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
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
    @Mock
    private ExecutorService aiExecutor;

    @InjectMocks
    private TaskService taskService;

    @Test
    void publishIndexesDraftAndRejectRemovesDraftFromIndexes() {
        var publishId = UUID.randomUUID();
        var publishTask = draftTask(publishId);
        when(taskRepository.findById(publishId)).thenReturn(Optional.of(publishTask));

        taskService.publish(publishId);

        assertThat(publishTask.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
        verify(similarityService).index(publishTask);
        verify(solutions).indexPublishedTask(publishTask);

        var rejectId = UUID.randomUUID();
        var rejectTask = draftTask(rejectId);
        when(taskRepository.findById(rejectId)).thenReturn(Optional.of(rejectTask));

        taskService.reject(rejectId);

        assertThat(rejectTask.getStatus()).isEqualTo(TaskStatus.REJECTED);
        verify(similarityService).remove(rejectId);
        verify(solutions).remove(rejectId);
    }

    @Test
    void publishingAlreadyPublishedTaskFails() {
        var taskId = UUID.randomUUID();
        var task = draftTask(taskId);
        task.publish();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.publish(taskId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("only DRAFT tasks can be published");
    }

    private TaskEntity draftTask() {
        return draftTask(UUID.randomUUID());
    }

    private TaskEntity draftTask(UUID id) {
        var mapper = new ObjectMapper();
        return TaskEntity.builder()
                .id(id)
                .title("Task")
                .description("Original description")
                .constraints(mapper.createArrayNode())
                .generatedByAi(true)
                .referenceSolutions(mapper.valueToTree(List.of("class Main {}")))
                .testCases(mapper.valueToTree(List.of(new Task.TestCase("0", "0"))))
                .concepts(mapper.createArrayNode())
                .status(TaskStatus.DRAFT)
                .build();
    }
}
