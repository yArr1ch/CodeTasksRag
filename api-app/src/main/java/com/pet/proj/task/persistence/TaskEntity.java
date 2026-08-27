package com.pet.proj.task.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.pet.proj.task.domain.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode constraints;

    @Column(nullable = false)
    private boolean generatedByAi;

    @Column(columnDefinition = "text")
    private String generationPrompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode referenceSolutions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode testCases;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode concepts;

    @Version
    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    public void setTestCases(JsonNode testCases) { this.testCases = testCases; }

    public void publish() {
        requireDraft("published");
        status = TaskStatus.PUBLISHED;
    }

    public void reject() {
        requireDraft("rejected");
        status = TaskStatus.REJECTED;
    }

    private void requireDraft(String action) {
        if (status != TaskStatus.DRAFT) {
            throw new IllegalStateException("only DRAFT tasks can be " + action);
        }
    }
}
