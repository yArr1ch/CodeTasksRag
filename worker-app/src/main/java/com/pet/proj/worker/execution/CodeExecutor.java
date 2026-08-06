package com.pet.proj.worker.execution;

import com.pet.proj.submission.domain.SubmissionStatus;

import java.util.List;

public interface CodeExecutor {
    ExecutionResult execute(ExecutionRequest request);

    record ExecutionRequest(String sourceCode, List<TestCase> testCases, boolean generateOutputs) {
        public ExecutionRequest(String sourceCode, List<TestCase> testCases) {
            this(sourceCode, testCases, false);
        }
    }

    record TestCase(String input, String expectedOutput) {
    }

    record ExecutionResult(SubmissionStatus status, int passedTests, int totalTests, String error,
                          List<String> outputs) {
    }
}
