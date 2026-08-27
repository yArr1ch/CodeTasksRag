package com.pet.proj.task.application;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public sealed interface TaskPrompt permits TaskHintPrompt, TaskRepairPrompt, TaskReviewPrompt, TaskGenerationPrompt {

    default String render() {
        switch (this) {
            case TaskHintPrompt thp -> {
                return load("prompts/task/hint-v1.txt").formatted(
                        thp.title(), thp.description(), thp.constraints(), thp.examples(), thp.concepts(),
                        thp.code(), thp.executionFeedback(), thp.knowledgeContext());
            }
            case TaskRepairPrompt trp -> {
                return load("prompts/task/repair-v1.txt").formatted(
                        trp.title(), trp.description(), trp.constraints(), trp.attempt(),
                        trp.failure().getMessage(), trp.failure().sourceCode());
            }
            case TaskReviewPrompt trp -> {
                return load("prompts/task/review-v1.txt").formatted(
                        trp.title(), trp.description(), trp.constraints(), trp.testCases(), trp.concepts());
            }
            case TaskGenerationPrompt tgp -> {
                return load("prompts/task/generation-v1.txt")
                        .formatted(tgp.prompt(), tgp.knowledgeContext());
            }
        }
    }

    private String load(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("could not load prompt resource: " + path, e);
        }
    }
}
