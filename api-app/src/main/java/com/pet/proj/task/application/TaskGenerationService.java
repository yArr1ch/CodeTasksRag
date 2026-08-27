package com.pet.proj.task.application;

import com.pet.proj.task.api.TaskGenerationRequest;
import com.pet.proj.task.api.TaskGenerationResponse;

import java.util.UUID;

public interface TaskGenerationService {

    TaskGenerationResponse generate(TaskGenerationRequest request);

    TaskGenerationResponse getGeneration(UUID id);

    void cancelGeneration(UUID id);
}
