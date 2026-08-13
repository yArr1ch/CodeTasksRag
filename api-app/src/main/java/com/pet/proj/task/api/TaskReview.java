package com.pet.proj.task.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TaskReview(boolean valid, @NotNull @Valid List<FieldWarning> warnings) {
    public TaskReview { warnings = List.copyOf(warnings == null ? List.of() : warnings); }

    public record FieldWarning(String field, Severity severity, String message,
                               String evidence, String impact, String suggestion) {
        public enum Severity { INFO, WARNING, ERROR }
    }
}
