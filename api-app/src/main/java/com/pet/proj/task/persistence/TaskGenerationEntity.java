package com.pet.proj.task.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.pet.proj.task.domain.TaskGenerationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "task_generations")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskGenerationEntity {
    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskGenerationStatus status;

    @Column(nullable = false)
    private int attempt;

    @Builder.Default
    @Column(nullable = false)
    private int maxAttempts = 3;

    private UUID taskId;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode similarTasks = JsonNodeFactory.instance.arrayNode();

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Version
    @Column(nullable = false)
    private int version;

    public void markProgress(TaskGenerationStatus status, int attempt) {
        this.status = status;
        this.attempt = attempt;
        this.errorMessage = null;
    }

    public void markSimilarTasks(JsonNode similarTasks) {
        this.status = TaskGenerationStatus.SIMILAR_TASKS_FOUND;
        this.similarTasks = similarTasks;
        this.errorMessage = null;
    }

    public void markReady(UUID taskId) {
        this.status = TaskGenerationStatus.READY;
        this.taskId = taskId;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = TaskGenerationStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markCanceled() {
        this.status = TaskGenerationStatus.CANCELED;
        this.errorMessage = "Task generation canceled by user";
    }

    public boolean isInProgress() {
        return switch (status) {
            case GENERATING, VALIDATING, REPAIRING -> true;
            default -> false;
        };
    }
}
