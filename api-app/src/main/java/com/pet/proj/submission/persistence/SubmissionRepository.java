package com.pet.proj.submission.persistence;

import com.pet.proj.submission.domain.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {
    List<SubmissionEntity> findByTaskIdAndStatus(UUID taskId, SubmissionStatus status);

    List<SubmissionEntity> findByTaskIdAndStatusAndExecutionMode(
            UUID taskId, SubmissionStatus status, String executionMode);
}
