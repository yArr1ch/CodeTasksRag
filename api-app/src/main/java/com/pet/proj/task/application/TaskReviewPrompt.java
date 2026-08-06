package com.pet.proj.task.application;

public record TaskReviewPrompt(String title, String description, String constraints,
                               String testCases, String concepts) {
    public String render() {
        return PromptResource.load("prompts/task/review-v1.txt")
                .formatted(title, description, constraints, testCases, concepts);
    }
}
