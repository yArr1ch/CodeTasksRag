package com.pet.proj.task.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.task.domain.Task;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskJsonMapper {
    private final ObjectMapper mapper;

    @Named("strings")
    public List<String> toStrings(JsonNode value) {
        return mapper.convertValue(value, new TypeReference<>() {
        });
    }

    @Named("testCases")
    public List<Task.TestCase> toTestCases(JsonNode value) {
        return mapper.convertValue(value, new TypeReference<>() {
        });
    }

    @Named("text")
    public String toText(JsonNode value) {
        return value == null ? "[]" : value.toString();
    }
}
