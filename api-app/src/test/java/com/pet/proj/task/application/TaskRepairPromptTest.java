package com.pet.proj.task.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskRepairPromptTest {
    @Test
    void render_failedReference_includesCompilerErrorAndSource() {
        var failure = new ReferenceSolutionException(
                "Main.java:5: error: cannot find symbol",
                "public class Main {}"
        );

        var prompt = new TaskRepairPrompt("base task prompt", failure, 2).render();

        assertThat(prompt)
                .contains("REPAIR ATTEMPT 2")
                .contains("cannot find symbol")
                .contains("public class Main {}")
                .contains("Return the complete source");
    }
}
