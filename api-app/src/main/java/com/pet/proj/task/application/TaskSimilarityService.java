package com.pet.proj.task.application;

import com.pet.proj.ai.SemanticSearchService;
import com.pet.proj.task.api.TaskGenerationResult;
import com.pet.proj.task.domain.TaskStatus;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskSimilarityService {
    private final TaskRepository taskRepository;
    private final SemanticSearchService semanticSearchService;

    @Value("${app.task-similarity.threshold:0.7}")
    private double similarityThreshold;

    @Value("${app.task-similarity.max-results:5}")
    private int maxResults = 5;

    public List<TaskGenerationResult.SimilarTask> findSimilarForGeneration(String query) {
        return findSimilar(query, null,
                "documentType == 'task' && status != 'REJECTED'",
                task -> task.getStatus() != TaskStatus.REJECTED);
    }

    private List<TaskGenerationResult.SimilarTask> findSimilar(
            String query,
            UUID excludedTaskId,
            String filter,
            Predicate<TaskEntity> eligibleTask) {
        var documents = semanticSearchService.search(
                query,
                maxResults + (excludedTaskId == null ? 0 : 1),
                similarityThreshold,
                filter);

        log.info("Task similarity search returned {} vector documents for queryLength={}", documents.size(), query.length());
        var taskIds = documents.stream()
                .map(this::taskId)
                .filter(Objects::nonNull)
                .filter(id -> !id.equals(excludedTaskId))
                .toList();
        var taskById = taskRepository.findAllById(taskIds).stream()
                .filter(eligibleTask)
                .collect(Collectors.toMap(TaskEntity::getId, Function.identity()));
        log.info("Task similarity mapped {} task IDs from {} vector documents", taskById.size(), documents.size());

        var matches = documents.stream()
                .map(document -> toSimilarTask(document, taskById))
                .filter(Objects::nonNull)
                .limit(maxResults)
                .toList();
        log.info("Task similarity produced {} task matches, threshold={}", matches.size(), similarityThreshold);
        return matches;
    }

    public void index(TaskEntity task) {
        if (task.getStatus() == TaskStatus.REJECTED) {
            return;
        }
        var taskId = task.getId().toString();
        semanticSearchService.replace(taskFilter(taskId), List.of(document(task)));
    }

    public int reindex() {
        var documents = taskRepository.findAll().stream()
                .filter(task -> task.getStatus() != TaskStatus.REJECTED)
                .map(this::document)
                .toList();
        semanticSearchService.replace("documentType == 'task'", documents);
        return documents.size();
    }

    public void remove(UUID taskId) {
        semanticSearchService.replace(taskFilter(taskId.toString()), List.of());
    }

    private Document document(TaskEntity task) {
        var taskId = task.getId().toString();
        return new Document(taskId, searchableText(task), Map.of(
                "documentType", "task",
                "taskId", taskId,
                "status", task.getStatus().name()));
    }

    private String taskFilter(String taskId) {
        return "documentType == 'task' && taskId == '" + taskId + "'";
    }

    private TaskGenerationResult.SimilarTask toSimilarTask(Document document, Map<UUID, TaskEntity> taskById) {
        var id = taskId(document);
        if (id == null) {
            return null;
        }
        var task = taskById.get(id);
        if (task == null) {
            return null;
        }
        return new TaskGenerationResult.SimilarTask(id, task.getTitle(), task.getDescription(), semanticSearchService.score(document));
    }

    private UUID taskId(Document document) {
        var value = document.getMetadata().get("taskId");
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String searchableText(TaskEntity task) {
        return String.join("\n",
                task.getTitle(),
                task.getDescription(),
                task.getConcepts().toString(),
                task.getConstraints().toString());
    }
}
