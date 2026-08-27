package com.pet.proj.task.messaging;

import com.pet.proj.contracts.TaskGenerationRequestedEvent;
import com.pet.proj.task.application.TaskGenerationProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile("messaging-kafka")
@RequiredArgsConstructor
class KafkaTaskGenerationListener {
    private final TaskGenerationProcessor processor;

    @RetryableTopic(
            attempts = "${app.kafka.task-generation-attempts:3}",
            backOff = @BackOff(delay = 1_000, multiplier = 2.0),
            dltTopicSuffix = ".DLT")
    @KafkaListener(
            topics = "${app.kafka.topics.task-generation-requested}",
            groupId = "algocoach-task-generation")
    void receive(TaskGenerationRequestedEvent event) {
        processor.processGeneration(event);
    }

    @DltHandler
    void handleDeadLetter(TaskGenerationRequestedEvent event, Exception failure) {
        processor.handleDeadLetter(event, failure);
    }
}
