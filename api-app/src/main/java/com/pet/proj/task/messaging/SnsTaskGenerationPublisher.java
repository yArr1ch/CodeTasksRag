package com.pet.proj.task.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.contracts.TaskGenerationRequestedEvent;
import com.pet.proj.task.application.TaskGenerationRequestPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
@Profile("messaging-sns-sqs")
@RequiredArgsConstructor
class SnsTaskGenerationPublisher implements TaskGenerationRequestPublisher {
    private final SnsAsyncClient sns;
    private final ObjectMapper objectMapper;

    @Value("${app.aws.sns.task-generation-topic-arn}")
    private String topicArn;

    @Override
    public CompletionStage<Void> publish(TaskGenerationRequestedEvent event) {
        final String message;
        try {
            message = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException failure) {
            return CompletableFuture.failedFuture(failure);
        }
        return sns.publish(PublishRequest.builder()
                        .topicArn(topicArn)
                        .message(message)
                        .build())
                .thenApply(_ -> (Void) null);
    }
}
