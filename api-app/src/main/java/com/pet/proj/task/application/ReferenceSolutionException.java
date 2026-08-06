package com.pet.proj.task.application;

final class ReferenceSolutionException extends RuntimeException {
    private final String sourceCode;

    ReferenceSolutionException(String message, String sourceCode) {
        super(message);
        this.sourceCode = sourceCode;
    }

    String sourceCode() {
        return sourceCode;
    }
}
