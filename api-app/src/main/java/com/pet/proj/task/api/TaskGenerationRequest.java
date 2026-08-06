package com.pet.proj.task.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskGenerationRequest(
        @NotBlank @Size(max = 2_000) String prompt,
        GenerationDecision decision
) {
    public TaskGenerationRequest {
        decision = decision == null ? GenerationDecision.CHECK : decision;
    }

    public enum GenerationDecision {
        CHECK,
        CONTINUE
    }
}
