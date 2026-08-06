package com.pet.proj.task.api;

import jakarta.validation.constraints.NotBlank;

public record TaskCorrectionRequest(@NotBlank String field, @NotBlank String correctedValue) {
}
