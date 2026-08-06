package com.pet.proj.task.application;

record TaskRepairPrompt(String basePrompt, ReferenceSolutionException failure, int attempt) {
    String render() {
        return PromptResource.load("prompts/task/repair-v1.txt")
                .formatted(basePrompt, attempt, failure.getMessage(), failure.sourceCode());
    }
}
