package com.pet.proj.task.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskHintRequest(
        @NotNull @Size(max = 20_000) String code,
        @NotNull @Size(max = 5_000) String executionFeedback
) {
}
