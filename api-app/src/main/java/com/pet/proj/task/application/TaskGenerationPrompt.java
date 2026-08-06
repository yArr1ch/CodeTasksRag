package com.pet.proj.task.application;

public record TaskGenerationPrompt(String prompt, String knowledgeContext) {
    public String render() {
        return PromptResource.load("prompts/task/generation-v1.txt")
                .formatted(prompt, knowledgeContext);
    }
}
