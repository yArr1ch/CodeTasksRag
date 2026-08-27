package com.pet.proj.points.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_task_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "user_task_progress_user_task_unique",
                columnNames = {"user_id", "task_id"}))
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTaskProgressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private boolean hintUsed;

    @Column(nullable = false)
    private boolean referenceUnlocked;

    @Column(nullable = false)
    private boolean passed;

    public void markHintUsed() {
        hintUsed = true;
    }

    public void unlockReference() {
        referenceUnlocked = true;
    }

    public void markPassed() {
        passed = true;
    }
}
