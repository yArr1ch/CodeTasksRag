package com.pet.proj.submission.application;

import com.pet.proj.contracts.SubmissionCreatedEvent;
import com.pet.proj.submission.domain.Submission;
import com.pet.proj.submission.domain.SubmissionStatus;
import com.pet.proj.submission.persistence.SubmissionEntity;
import com.pet.proj.submission.persistence.SubmissionRepository;
import com.pet.proj.user.application.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {
    @Mock
    private SubmissionRepository submissions;

    @Mock
    private SubmissionMapper mapper;

    @Mock
    private KafkaTemplate<String, SubmissionCreatedEvent> events;

    @Mock
    private UserAccountService userAccounts;

    private SubmissionService service;

    @BeforeEach
    void setUp() {
        service = new SubmissionService(submissions, mapper, events, userAccounts);
        ReflectionTestUtils.setField(service, "submissionCreatedTopic", "submission-created");
    }

    @Test
    void standardSubmissionReturnsImmediatelyAsQueued() {
        var taskId = UUID.randomUUID();
        var submissionId = UUID.randomUUID();
        var entity = SubmissionEntity.builder()
                .id(submissionId)
                .taskId(taskId)
                .sourceCode("class Main {}")
                .executionMode(SubmissionCreatedEvent.STANDARD)
                .status(SubmissionStatus.QUEUED)
                .build();
        var expected = new Submission(submissionId, taskId, entity.getSourceCode(),
                SubmissionStatus.QUEUED, 0, 0, null);
        when(submissions.save(any())).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(expected);
        when(events.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        var result = service.submit(taskId, entity.getSourceCode());

        assertThat(result).isEqualTo(expected);
        verify(events).send(any(), any(), any());
    }
}
