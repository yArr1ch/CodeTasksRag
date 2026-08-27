package com.pet.proj.task.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.task.domain.Task;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskJsonMapperTest {
    private final TaskJsonMapper mapper = new TaskJsonMapper(new ObjectMapper());

    @Test
    void toStrings_jsonArray_returnsStringValues() throws Exception {
        var value = new ObjectMapper().readTree("[\"arrays\",\"sorting\"]");

        assertThat(mapper.toStrings(value)).containsExactly("arrays", "sorting");
    }

    @Test
    void toTestCases_jsonArray_returnsTaskTestCases() throws Exception {
        var value = new ObjectMapper().readTree("[{\"input\":\"3\\n1 2 3\",\"expectedOutput\":\"6\"}]");

        assertThat(mapper.toTestCases(value))
                .containsExactly(new Task.TestCase("3\n1 2 3", "6"));
    }
}
