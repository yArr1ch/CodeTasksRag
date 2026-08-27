package com.pet.proj.task.application;

import com.pet.proj.contracts.TaskGenerationRequestedEvent;

import java.util.concurrent.CompletionStage;

public interface TaskGenerationRequestPublisher {

    CompletionStage<Void> publish(TaskGenerationRequestedEvent event);
}
