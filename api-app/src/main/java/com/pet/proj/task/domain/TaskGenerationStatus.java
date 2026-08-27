package com.pet.proj.task.domain;

public enum TaskGenerationStatus {
    GENERATING,
    VALIDATING,
    REPAIRING,
    SIMILAR_TASKS_FOUND,
    READY,
    FAILED,
    CANCELED
}
