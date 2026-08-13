package com.pet.proj.task.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
class TaskHintPromptTest {
    @Test
    void render_includesCurrentCodeAndExecutionFeedback() {
        var rendered = new TaskHintPrompt(
                "Task", "Description", "[]", "[]", "[]", "class Main {}", "1 test failed", "context")
                .render();

        assertThat(rendered)
                .contains("class Main {}")
                .contains("1 test failed")
                .contains("one concrete next action")
                .doesNotContain("RingShift");
    }
}
