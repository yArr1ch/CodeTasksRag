package com.pet.proj.task.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.proj.contracts.TaskGenerationRequestedEvent;
import com.pet.proj.task.application.TaskGenerationProcessor;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Profile("messaging-sns-sqs")
@RequiredArgsConstructor
class SqsTaskGenerationListener {
    private final SqsAsyncClient sqs;
    private final ObjectMapper objectMapper;
    private final TaskGenerationProcessor processor;
    private final ExecutorService aiExecutor;

    @Value("${app.aws.sqs.task-generation-queue-url}")
    private String queueUrl;

    @Value("${app.aws.sqs.task-generation-dead-letter-queue-url}")
    private String deadLetterQueueUrl;

    @Value("${app.aws.sqs.wait-time-seconds:20}")
    private int waitTimeSeconds;

    @Value("${app.aws.sqs.poll-delay:1s}")
    private Duration pollDelay;

    private volatile boolean running;

    @EventListener(ApplicationReadyEvent.class)
    void startPolling() {
        running = true;
        poll(queueUrl, false);
        poll(deadLetterQueueUrl, true);
    }

    @PreDestroy
    void stopPolling() {
        running = false;
    }

    private void poll(String sourceQueueUrl, boolean deadLetter) {
        if (!running) {
            return;
        }
        sqs.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(sourceQueueUrl)
                        .maxNumberOfMessages(1)
                        .waitTimeSeconds(waitTimeSeconds)
                        .build())
                .whenComplete((response, failure) -> {
                    if (failure != null) {
                        log.warn("SQS polling failed, queue={}, message={}", sourceQueueUrl, failure.getMessage());
                        scheduleNextPoll(sourceQueueUrl, deadLetter);
                        return;
                    }
                    var message = response.messages().stream().findFirst();
                    if (message.isEmpty()) {
                        poll(sourceQueueUrl, deadLetter);
                        return;
                    }
                    process(message.get(), sourceQueueUrl, deadLetter)
                            .whenComplete((_, processingFailure) -> {
                                if (processingFailure != null) {
                                    log.error("SQS task generation message will be retried, queue={}",
                                            sourceQueueUrl, processingFailure);
                                }
                                scheduleNextPoll(sourceQueueUrl, deadLetter);
                            });
                });
    }

    private CompletionStage<Void> process(Message message, String sourceQueueUrl, boolean deadLetter) {
        return CompletableFuture.runAsync(() -> processMessage(message, deadLetter), aiExecutor)
                .thenCompose(_ -> sqs.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(sourceQueueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build()))
                .thenApply(_ -> (Void) null);
    }

    private void processMessage(Message message, boolean deadLetter) {
        final TaskGenerationRequestedEvent event;
        try {
            event = objectMapper.readValue(message.body(), TaskGenerationRequestedEvent.class);
        } catch (JsonProcessingException failure) {
            if (deadLetter) {
                log.error("Dropping malformed task generation message from SQS DLQ", failure);
                return;
            }
            throw new CompletionException(failure);
        }

        if (deadLetter) {
            processor.handleDeadLetter(event,
                    new IllegalStateException("task generation message exceeded SQS redrive limit"));
        } else {
            processor.processGeneration(event);
        }
    }

    private void scheduleNextPoll(String sourceQueueUrl, boolean deadLetter) {
        if (running) {
            CompletableFuture.delayedExecutor(pollDelay.toMillis(), TimeUnit.MILLISECONDS)
                    .execute(() -> poll(sourceQueueUrl, deadLetter));
        }
    }
}
