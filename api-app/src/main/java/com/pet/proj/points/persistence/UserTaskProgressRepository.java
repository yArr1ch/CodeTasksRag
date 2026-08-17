package com.pet.proj.points.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserTaskProgressRepository extends JpaRepository<UserTaskProgressEntity, UUID> {
    Optional<UserTaskProgressEntity> findByUserIdAndTaskId(UUID userId, UUID taskId);
}
