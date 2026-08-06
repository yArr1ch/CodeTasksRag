package com.pet.proj.task.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TaskHintRequest(
        @Min(1) @Max(3) int level
) {
}
