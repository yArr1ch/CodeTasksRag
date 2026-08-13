package com.pet.proj.task.application;

public record TaskGenerationPrompt(String prompt, String knowledgeContext) implements TaskPrompt {
}
