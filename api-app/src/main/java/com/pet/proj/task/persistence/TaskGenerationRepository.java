package com.pet.proj.task.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskGenerationRepository extends JpaRepository<TaskGenerationEntity, UUID> {
}
