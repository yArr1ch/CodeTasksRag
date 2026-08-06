package com.pet.proj.task.application;

import com.pet.proj.ai.SemanticSearchService;
import com.pet.proj.task.api.TaskGenerationResult;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskSimilarityService {
    private final TaskRepository tasks;
    private final SemanticSearchService semanticSearch;

    @Value("${app.task-similarity.threshold}")
    private double similarityThreshold;

    public List<TaskGenerationResult.SimilarTask> findSimilar(String query) {
        return findSimilar(query, null);
    }

    public List<TaskGenerationResult.SimilarTask> findSimilar(String query, UUID excludedTaskId) {
        var limit = 5;
        var documents = semanticSearch.search(
                query,
                limit + (excludedTaskId == null ? 0 : 1),
                similarityThreshold,
                "documentType == 'task'");

        log.info("Task similarity search returned {} vector documents for queryLength={}", documents.size(), query.length());
        var taskIds = documents.stream()
                .map(this::taskId)
                .filter(java.util.Objects::nonNull)
                .filter(id -> !id.equals(excludedTaskId))
                .toList();
        var taskById = tasks.findAllById(taskIds).stream()
                .collect(Collectors.toMap(TaskEntity::getId, Function.identity()));
        log.info("Task similarity mapped {} task IDs from {} vector documents", taskById.size(), documents.size());

        var matches = documents.stream()
                .map(document -> toSimilarTask(document, taskById))
                .filter(java.util.Objects::nonNull)
                .limit(limit)
                .toList();
        log.info("Task similarity produced {} task matches, threshold={}", matches.size(), similarityThreshold);
        return matches;
    }

    public List<TaskGenerationResult.SimilarTask> findSimilar(TaskEntity task) {
        return findSimilar(searchableText(task), task.getId());
    }

    public void index(TaskEntity task) {
        var taskId = task.getId().toString();
        var document = new Document(
                taskId,
                searchableText(task),
                Map.of(
                        "documentType", "task",
                        "taskId", taskId,
                        "status", task.getStatus().name()));
        semanticSearch.replace(List.of(document));
    }

    @Transactional
    public int reindex() {
        var allTasks = tasks.findAll();
        allTasks.forEach(this::index);
        return allTasks.size();
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
        return new TaskGenerationResult.SimilarTask(id, task.getTitle(), task.getDescription(), semanticSearch.score(document));
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
