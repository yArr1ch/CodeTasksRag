package com.pet.proj.worker.execution;

import com.pet.proj.submission.domain.SubmissionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DockerCodeExecutor implements CodeExecutor {
    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(5);

    @Value("${app.execution.docker-image:algocoach-java-runner:25}")
    private final String image;

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        log.info("Starting execution: tests={}", request.testCases().size());
        try {
            var directory = Files.createTempDirectory("algocoach-submission-");
            log.debug("Execution directory: {}", directory);
            try {
                writeSource(directory, request.sourceCode());
                var compilation = compile(directory);
                log.info("Compilation: exitCode={}, timedOut={}", compilation.exitCode(), compilation.timedOut());
                if (!compilation.success()) {
                    return failed(SubmissionStatus.COMPILE_ERROR, request, compilation.output());
                }

                var passed = 0;
                var outputs = new ArrayList<String>();
                for (var index = 0; index < request.testCases().size(); index++) {
                    var testCase = request.testCases().get(index);
                    log.info("Test {}/{} input={}", index + 1, request.testCases().size(), escaped(testCase.input()));
                    var result = run(directory, testCase.input());
                    log.info("Test {}/{} result: exitCode={}, timedOut={}, actual={}, expected={}",
                            index + 1, request.testCases().size(), result.exitCode(), result.timedOut(),
                            escaped(result.output()), escaped(testCase.expectedOutput()));
                    if (result.timedOut()) {
                        return new ExecutionResult(SubmissionStatus.TIMEOUT, passed,
                                request.testCases().size(), "execution timed out", outputs);
                    }
                    if (!result.success()) {
                        return failed(SubmissionStatus.FAILED, request,
                                "test input: " + escaped(testCase.input()) + "\n"
                                        + result.output());
                    }
                    if (request.generateOutputs()) {
                        outputs.add(result.output());
                        passed++;
                        continue;
                    }
                    if (testCase.expectedOutput() == null) {
                        return failed(SubmissionStatus.FAILED, request, "task test case has no expected output");
                    }
                    if (!normalize(result.output()).equals(normalize(testCase.expectedOutput()))) {
                        return failed(SubmissionStatus.FAILED, request, "wrong answer");
                    }
                    passed++;
                }
                return new ExecutionResult(SubmissionStatus.PASSED, passed,
                        request.testCases().size(), null, outputs);
            } finally {
                deleteDirectory(directory);
            }
        } catch (Exception e) {
            log.error("Execution failed unexpectedly", e);
            return failed(SubmissionStatus.FAILED, request, e.getMessage());
        }
    }

    private ProcessResult compile(Path directory) throws IOException, InterruptedException {
        return runDocker(directory, List.of("javac", "Main.java"), "");
    }

    private ProcessResult run(Path directory, String input) throws IOException, InterruptedException {
        return runDocker(directory, List.of("java", "Main"), input);
    }

    private ProcessResult runDocker(Path directory, List<String> command, String input)
            throws IOException, InterruptedException {
        var dockerCommand = new ArrayList<>(List.of(
                "docker", "run", "--rm", "--interactive", "--network", "none",
                "--memory", "256m", "--cpus", "1", "--pids-limit", "64",
                "--read-only", "--tmpfs", "/tmp:rw,noexec,nosuid,size=16m",
                "--cap-drop", "ALL", "--security-opt", "no-new-privileges",
                "--user", "1000:1000",
                "-v", directory.toAbsolutePath() + ":/workspace:rw",
                "-w", "/workspace", image));
        dockerCommand.addAll(command);

        log.debug("Starting Docker command={}, input={}", command, escaped(input));
        var process = new ProcessBuilder(dockerCommand).redirectErrorStream(true).start();
        try (var output = process.getOutputStream()) {
            output.write(input.getBytes(StandardCharsets.UTF_8));
        }
        if (!process.waitFor(EXECUTION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
            return new ProcessResult(-1, "execution timed out", true);
        }
        var result = new ProcessResult(process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8), false);
        log.debug("Docker process finished: exitCode={}", result.exitCode());
        return result;
    }

    private ExecutionResult failed(SubmissionStatus status, ExecutionRequest request, String error) {
        return new ExecutionResult(status, 0, request.testCases().size(), error, List.of());
    }

    private void writeSource(Path directory, String sourceCode) throws IOException {
        Files.writeString(directory.resolve("Main.java"), sourceCode, StandardCharsets.UTF_8);
    }

    private String normalize(String output) {
        return output.replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("(?m)[ \\t]+$", "")
                .stripTrailing();
    }

    private String escaped(String value) {
        return value == null ? "<null>" : value.replace("\\", "\\\\")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private void deleteDirectory(Path directory) {
        try (var files = Files.walk(directory)) {
            files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private record ProcessResult(int exitCode, String output, boolean timedOut) {
        boolean success() {
            return !timedOut && exitCode == 0;
        }
    }
}
