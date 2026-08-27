package com.pet.proj.submission.api;

import com.pet.proj.submission.application.SubmissionService;
import com.pet.proj.submission.domain.Submission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService service;

    @PostMapping
    public Submission submit(@Valid @RequestBody SubmitRequest request) {
        return service.submit(request.taskId(), request.sourceCode());
    }

    @GetMapping("/{id}")
    public Submission get(@PathVariable UUID id) {
        return service.get(id);
    }

    public record SubmitRequest(@NotNull UUID taskId, @NotBlank String sourceCode) {
    }
}
