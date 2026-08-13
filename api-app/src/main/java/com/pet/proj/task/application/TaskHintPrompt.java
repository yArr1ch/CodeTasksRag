package com.pet.proj.task.application;

public record TaskHintPrompt(String title, String description, String constraints, String examples, String concepts,
                             String code, String executionFeedback, String knowledgeContext) implements TaskPrompt {
}
