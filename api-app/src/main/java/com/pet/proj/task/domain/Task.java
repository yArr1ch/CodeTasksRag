package com.pet.proj.task.domain;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record Task(
        UUID id,
        @NotBlank String title,
        String description,
        List<String> constraints,
        List<TestCase> testCases,
        TaskStatus status
) {

    public record TestCase(String input, String expectedOutput) {
    }
}
