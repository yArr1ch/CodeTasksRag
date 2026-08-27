package com.pet.proj.task.messaging;

import com.pet.proj.contracts.TaskGenerationRequestedEvent;
import com.pet.proj.task.application.TaskGenerationRequestPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionStage;

@Component
@Profile("messaging-kafka")
@RequiredArgsConstructor
class KafkaTaskGenerationPublisher implements TaskGenerationRequestPublisher {

    private final KafkaTemplate<String, TaskGenerationRequestedEvent> events;

    @Value("${app.kafka.topics.task-generation-requested}")
    private String topic;

    @Override
    public CompletionStage<Void> publish(TaskGenerationRequestedEvent event) {
        return events.send(topic, event.generationId().toString(), event)
                .thenApply(_ -> null);
    }
}
