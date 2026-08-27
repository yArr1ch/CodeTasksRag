package com.pet.proj.task.application;

public record TaskReviewPrompt(String title, String description, String constraints, String testCases, String concepts)
        implements TaskPrompt {
}
