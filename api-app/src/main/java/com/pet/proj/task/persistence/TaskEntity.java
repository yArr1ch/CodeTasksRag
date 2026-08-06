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

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setConstraints(JsonNode constraints) { this.constraints = constraints; }
    public void setReferenceSolutions(JsonNode referenceSolutions) { this.referenceSolutions = referenceSolutions; }
    public void setTestCases(JsonNode testCases) { this.testCases = testCases; }
    public void setConcepts(JsonNode concepts) { this.concepts = concepts; }
}
