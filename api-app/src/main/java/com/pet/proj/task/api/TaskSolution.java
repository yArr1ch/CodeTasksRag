package com.pet.proj.task.api;

import java.util.UUID;

public record TaskSolution(
        UUID id,
        String sourceCode,
        SolutionType type,
        double similarity
) {
    public enum SolutionType {
        REFERENCE,
        COMMUNITY
    }
}
