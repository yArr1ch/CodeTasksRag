package com.pet.proj.task.api;

import com.pet.proj.task.application.TaskService;
import com.pet.proj.task.application.TaskSolutionService;
import com.pet.proj.task.domain.Task;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;
    private final TaskSolutionService solutions;

    @GetMapping
    public List<Task> getAll() {
        return service.getPublishedTasks();
    }

    @GetMapping("/{id}")
    public Task get(@PathVariable UUID id) {
        return service.getTask(id);
    }

    @PostMapping("/generate")
    public CompletableFuture<TaskGenerationResult> generate(@Valid @RequestBody TaskGenerationRequest request) {
        return service.generate(request);
    }

    // internal
    @PostMapping("/embeddings/reindex")
    public Map<String, Integer> reindexEmbeddings() {
        return Map.of("indexed", service.reindexTaskEmbeddings());
    }

    @PostMapping("/{id}/review")
    public CompletableFuture<TaskReview> review(@PathVariable UUID id) {
        return service.review(id);
    }

    @PatchMapping("/{id}/corrections")
    public Task applyCorrection(@PathVariable UUID id, @Valid @RequestBody TaskCorrectionRequest request) {
        return service.applyCorrection(id, request);
    }

    @PostMapping("/{id}/hints")
    public CompletableFuture<TaskHintResponse> hint(@PathVariable UUID id, @Valid @RequestBody TaskHintRequest request) {
        return service.generateHint(id, request);
    }

    @GetMapping("/{id}/solutions")
    public List<TaskSolution> solutions(@PathVariable UUID id, @RequestParam(required = false) String query) {
        return solutions.findSolutions(id, query);
    }

    @PostMapping("/{id}/publish")
    @ResponseStatus(HttpStatus.OK)
    public void publish(@PathVariable UUID id) {
        service.publish(id);
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    public void reject(@PathVariable UUID id) {
        service.reject(id);
    }
}
