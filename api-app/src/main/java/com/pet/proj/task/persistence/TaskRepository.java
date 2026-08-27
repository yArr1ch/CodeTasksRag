package com.pet.proj.task.persistence;

import com.pet.proj.task.domain.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
    List<TaskEntity> findByStatusOrderByIdAsc(TaskStatus status, Pageable pageable);
    List<TaskEntity> findByStatusAndIdGreaterThanOrderByIdAsc(TaskStatus status, UUID cursor, Pageable pageable);
}
