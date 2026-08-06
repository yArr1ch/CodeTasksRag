package com.pet.proj.task.application;

import com.pet.proj.task.api.TaskSolution;
import com.pet.proj.ai.SemanticSearchService;
import com.pet.proj.contracts.SubmissionCreatedEvent;
import com.pet.proj.submission.domain.SubmissionStatus;
import com.pet.proj.submission.persistence.SubmissionRepository;
import com.pet.proj.task.persistence.TaskEntity;
import com.pet.proj.task.persistence.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    @Value("${app.solution-similarity.threshold}")
    private double similarityThreshold;

    public List<TaskSolution> findSolutions(UUID taskId, String query) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("task not found"));
        indexSolutions(task);
        var searchText = query == null || query.isBlank()
                ? taskSearchText(task)
                : query;

        var limit = 5;
        var scores = semanticSearchService.search(
                searchText,
                limit,
                similarityThreshold,
                "documentType == 'solution' && taskId == '" + taskId + "' && status == 'PASSED'");

        return scores.stream()
                .filter(d -> d.getText() != null)
                .map(document -> new TaskSolution(
                        UUID.fromString(document.getMetadata().get("solutionId").toString()),
                        document.getText().substring(document.getText().indexOf("\n\n") + 2),
                        TaskSolution.SolutionType.valueOf(document.getMetadata().get("solutionType").toString()),
                        semanticSearchService.score(document)))
                .limit(limit)
                .toList();
    }

    private void indexSolutions(TaskEntity task) {
        var documents = new ArrayList<Document>();
        var ids = new ArrayList<String>();

        if (task.getReferenceSolutions() != null && task.getReferenceSolutions().isArray()) {
            for (var index = 0; index < task.getReferenceSolutions().size(); index++) {
                var solutionId = UUID.nameUUIDFromBytes(
                        (task.getId() + ":reference:" + index).getBytes(StandardCharsets.UTF_8));
                documents.add(document(task, solutionId, task.getReferenceSolutions().get(index).asText(),
                        TaskSolution.SolutionType.REFERENCE));
                ids.add(solutionId.toString());
            }
        }

        var passedSubmissions = submissionRepository.findByTaskIdAndStatusAndExecutionMode(
                task.getId(), SubmissionStatus.PASSED, SubmissionCreatedEvent.STANDARD);
        for (var submission : passedSubmissions) {
            documents.add(document(task, submission.getId(), submission.getSourceCode(),
                    TaskSolution.SolutionType.COMMUNITY));
            ids.add(submission.getId().toString());
        }

        if (!ids.isEmpty()) {
            semanticSearchService.replace(documents);
        }
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
}
