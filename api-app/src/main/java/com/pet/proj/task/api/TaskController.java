package com.pet.proj.task.api;

import com.pet.proj.task.application.TaskGenerationService;
import com.pet.proj.task.application.TaskService;
import com.pet.proj.task.application.TaskSolutionService;
import com.pet.proj.task.domain.Task;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks")
@Validated
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskSolutionService taskSolutionService;
    private final TaskGenerationService taskGenerationService;

    @GetMapping
    public TaskPageResponse getAll(
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int limit,
            @RequestParam(required = false) UUID cursor) {
        return taskService.getPublishedTasks(limit, cursor);
    }

    @GetMapping("/{id}")
    public Task get(@PathVariable UUID id) {
        return taskService.getTask(id);
    }

    @PostMapping("/generate")
    public TaskGenerationResponse generate(@Valid @RequestBody TaskGenerationRequest request) {
        return taskGenerationService.generate(request);
    }

    @GetMapping("/generations/{id}")
    public TaskGenerationResponse getGeneration(@PathVariable UUID id) {
        return taskGenerationService.getGeneration(id);
    }

    @PostMapping("/generations/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelGeneration(@PathVariable UUID id) {
        taskGenerationService.cancelGeneration(id);
    }

    // internal
    @PostMapping("/embeddings/reindex")
    public Map<String, Integer> reindexEmbeddings() {
        return Map.of(
                "tasksIndexed", taskService.reindexTaskEmbeddings(),
                "solutionsIndexed", taskSolutionService.reindex());
    }

    @PostMapping("/{id}/review")
    public CompletableFuture<TaskReview> review(@PathVariable UUID id) {
        return taskService.review(id);
    }

    @PostMapping("/{id}/hints")
    public CompletableFuture<TaskHintResponse> hint(@PathVariable UUID id, @Valid @RequestBody TaskHintRequest request) {
        return taskService.generateHint(id, request);
    }

    @GetMapping("/{id}/solutions")
    public List<TaskSolution> solutions(@PathVariable UUID id, @RequestParam(required = false) String query) {
        return taskSolutionService.findSolutions(id, query);
    }

    @PostMapping("/{id}/publish")
    @ResponseStatus(HttpStatus.OK)
    public void publish(@PathVariable UUID id) {
        taskService.publish(id);
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    public void reject(@PathVariable UUID id) {
        taskService.reject(id);
    }
}
