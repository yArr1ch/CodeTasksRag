package com.pet.proj.task.application;

import com.pet.proj.task.api.TaskSolution;
import com.pet.proj.ai.SemanticSearchService;
import com.pet.proj.contracts.SubmissionCompletedEvent;
import com.pet.proj.contracts.SubmissionCreatedEvent;
import com.pet.proj.task.domain.TaskStatus;
import com.pet.proj.submission.domain.SubmissionStatus;
import com.pet.proj.submission.persistence.SubmissionRepository;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskSolutionService {
    private final TaskRepository taskRepository;
    private final SubmissionRepository submissionRepository;
    private final SemanticSearchService semanticSearchService;

    @Value("${app.solution-similarity.threshold:0.0}")
    private double similarityThreshold;

    @Value("${app.solution-similarity.max-results:5}")
    private int maxResults = 5;

    public List<TaskSolution> findSolutions(UUID taskId, String query) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("task not found"));
        if (task.getStatus() == TaskStatus.DRAFT) {
            return referenceSolutions(task);
        }
        if (task.getStatus() != TaskStatus.PUBLISHED) {
            return List.of();
        }
        var searchText = query == null || query.isBlank()
                ? taskSearchText(task)
                : query;

        var scores = semanticSearchService.search(
                searchText,
                maxResults,
                similarityThreshold,
                "documentType == 'solution' && taskId == '" + taskId + "' && status == 'PASSED'");

        var solutionIds = new HashSet<UUID>();
        return scores.stream()
                .filter(d -> d.getText() != null)
                .map(document -> new TaskSolution(
                        UUID.fromString(document.getMetadata().get("solutionId").toString()),
                        document.getText().substring(document.getText().indexOf("\n\n") + 2),
                        TaskSolution.SolutionType.valueOf(document.getMetadata().get("solutionType").toString()),
                        semanticSearchService.score(document)))
                .filter(solution -> solutionIds.add(solution.id()))
                .limit(maxResults)
                .toList();
    }

    public void indexPublishedTask(TaskEntity task) {
        if (task.getStatus() == TaskStatus.PUBLISHED) {
            indexSolutions(task);
        }
    }

    public int reindex() {
        var documents = taskRepository.findAll().stream()
                .filter(task -> task.getStatus() == TaskStatus.PUBLISHED)
                .flatMap(task -> solutionDocuments(task).stream())
                .toList();
        semanticSearchService.replace("documentType == 'solution'", documents);
        return documents.size();
    }

    public void remove(UUID taskId) {
        semanticSearchService.replace(solutionFilter(taskId), List.of());
    }

    @KafkaListener(
            topics = "${app.kafka.topics.submission-completed}",
            groupId = "algocoach-task-solutions")
    public void handleSubmissionCompleted(SubmissionCompletedEvent event) {
        if (!SubmissionCreatedEvent.STANDARD.equals(event.executionMode())
                || !SubmissionStatus.PASSED.name().equals(event.status())) {
            return;
        }
        taskRepository.findById(event.taskId())
                .filter(task -> task.getStatus() == TaskStatus.PUBLISHED)
                .ifPresent(this::indexSolutions);
    }

    private List<TaskSolution> referenceSolutions(TaskEntity task) {
        if (task.getReferenceSolutions() == null || !task.getReferenceSolutions().isArray()) {
            return List.of();
        }
        var solutions = new ArrayList<TaskSolution>();
        for (var index = 0; index < task.getReferenceSolutions().size(); index++) {
            var id = UUID.nameUUIDFromBytes(
                    (task.getId() + ":reference:" + index).getBytes(StandardCharsets.UTF_8));
            solutions.add(new TaskSolution(
                    id,
                    task.getReferenceSolutions().get(index).asText(),
                    TaskSolution.SolutionType.REFERENCE,
                    1.0));
        }
        return solutions;
    }

    private void indexSolutions(TaskEntity task) {
        semanticSearchService.replace(solutionFilter(task.getId()), solutionDocuments(task));
    }

    private List<Document> solutionDocuments(TaskEntity task) {
        var documents = new ArrayList<Document>();
        if (task.getReferenceSolutions() != null && task.getReferenceSolutions().isArray()) {
            for (var index = 0; index < task.getReferenceSolutions().size(); index++) {
                var solutionId = UUID.nameUUIDFromBytes(
                        (task.getId() + ":reference:" + index).getBytes(StandardCharsets.UTF_8));
                documents.add(document(task, solutionId, task.getReferenceSolutions().get(index).asText(),
                        TaskSolution.SolutionType.REFERENCE));
            }
        }

        var passedSubmissions = submissionRepository.findByTaskIdAndStatusAndExecutionMode(
                task.getId(), SubmissionStatus.PASSED, SubmissionCreatedEvent.STANDARD);
        for (var submission : passedSubmissions) {
            documents.add(document(task, submission.getId(), submission.getSourceCode(),
                    TaskSolution.SolutionType.COMMUNITY));
        }

        return documents;
    }

    private Document document(TaskEntity task, UUID solutionId, String sourceCode,
                              TaskSolution.SolutionType type) {
        return new Document(
                solutionId.toString(),
                taskSearchText(task) + "\n\n" + sourceCode,
                Map.of(
                        "documentType", "solution",
                        "taskId", task.getId().toString(),
                        "solutionId", solutionId.toString(),
                        "solutionType", type.name(),
                        "status", "PASSED"));
    }

    private String taskSearchText(TaskEntity task) {
        return String.join("\n",
                task.getTitle(),
                task.getDescription(),
                task.getConcepts().toString(),
                task.getConstraints().toString());
    }

    private String solutionFilter(UUID taskId) {
        return "documentType == 'solution' && taskId == '" + taskId + "'";
    }
}
