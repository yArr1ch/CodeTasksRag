package com.pet.proj.common;

import com.pet.proj.ai.AiGenerationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void aiFailure_returnsBadGateway() {
        var problem = handler.aiFailure(new AiGenerationException(
                "AI generation failed",
                null));

        assertThat(problem.getStatus()).isEqualTo(502);
        assertThat(problem.getDetail()).isEqualTo("AI generation failed");
    }

    @Test
    void duplicateKnowledgeDocument_returnsConflict() {
        var problem = handler.dataIntegrityViolation(new DataIntegrityViolationException(
                "insert failed",
                new RuntimeException("knowledge_documents_title_source_unique_idx")));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail())
                .isEqualTo("A knowledge document with this title and source already exists.");
    }
}
