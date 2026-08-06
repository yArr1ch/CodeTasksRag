package com.pet.proj.task.application;

public record TaskHintPrompt(
        String title,
        String description,
        String constraints,
        String examples,
        String concepts,
        int level,
        String knowledgeContext
) {
    public String render() {
        return PromptResource.load("prompts/task/hint-v1.txt")
                .formatted(title, description, constraints, examples, concepts, level, levelGuidance(), knowledgeContext);
    }

    private String levelGuidance() {
        return switch (level) {
            case 1 -> "Give only a broad conceptual direction. Do not mention a concrete algorithm or code.";
            case 2 -> "Identify the relevant algorithm or pattern, explain the key observation, and describe the solution steps and important edge cases. Do not provide code.";
            case 3 -> "Give detailed pseudocode and implementation guidance, but do not provide a complete solution.";
            default -> throw new IllegalArgumentException("hint level must be between 1 and 3");
        };
    }
}
