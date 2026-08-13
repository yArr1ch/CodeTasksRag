package com.pet.proj.task.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.task.api.TaskGenerationResult;
import com.pet.proj.task.domain.TaskStatus;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSimilarityServiceTest {
    @Mock
    private TaskRepository tasks;
    @Mock
    private com.pet.proj.ai.SemanticSearchService semanticSearch;

    @Test
    void reindexReplacesPublishedAndDraftTaskDocuments() {
        var service = service();
        when(tasks.findAll()).thenReturn(List.of(
                task(UUID.randomUUID(), TaskStatus.PUBLISHED),
                task(UUID.randomUUID(), TaskStatus.DRAFT),
                task(UUID.randomUUID(), TaskStatus.REJECTED)));

        assertThat(service.reindex()).isEqualTo(2);
        verify(semanticSearch).replace(eq("documentType == 'task'"), anyList());
    }

    @Test
    void generationSearchIncludesDraftTasksButExcludesRejectedTasks() {
        var service = service();
        var draftId = UUID.randomUUID();
        var draft = task(draftId, TaskStatus.DRAFT);
        var document = new Document(draftId.toString(), "draft", Map.of(
                "documentType", "task", "taskId", draftId.toString(), "status", "DRAFT"));
        when(semanticSearch.search("query", 5, 0.7,
                "documentType == 'task' && status != 'REJECTED'"))
                .thenReturn(List.of(document));
        when(tasks.findAllById(List.of(draftId))).thenReturn(List.of(draft));

        assertThat(service.findSimilarForGeneration("query"))
                .extracting(TaskGenerationResult.SimilarTask::id)
                .containsExactly(draftId);
    }

    @Test
    void indexDraftTask_addsItToDuplicateSearchCandidates() {
        var service = service();
        var taskId = UUID.randomUUID();
        var draft = task(taskId, TaskStatus.DRAFT);

        service.index(draft);

        verify(semanticSearch).replace(
                eq("documentType == 'task' && taskId == '" + taskId + "'"), anyList());
    }

    private TaskSimilarityService service() {
        var service = new TaskSimilarityService(tasks, semanticSearch);
        ReflectionTestUtils.setField(service, "similarityThreshold", 0.7);
        return service;
    }

    private TaskEntity task(UUID id, TaskStatus status) {
        var mapper = new ObjectMapper();
        return TaskEntity.builder()
                .id(id)
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
