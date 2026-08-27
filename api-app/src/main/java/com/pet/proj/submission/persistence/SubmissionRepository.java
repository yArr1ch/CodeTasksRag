package com.pet.proj.submission.persistence;

import com.pet.proj.submission.domain.SubmissionStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {
    List<SubmissionEntity> findByTaskIdAndStatusAndExecutionMode(
            UUID taskId, SubmissionStatus status, String executionMode);

    @Modifying
    @Transactional
    @Query("update SubmissionEntity s set s.status = :running "
            + "where s.id = :id and s.status = :queued")
    int claimForExecution(@Param("id") UUID id,
                          @Param("queued") SubmissionStatus queued,
                          @Param("running") SubmissionStatus running);
}
