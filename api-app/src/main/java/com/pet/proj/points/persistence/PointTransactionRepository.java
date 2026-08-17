package com.pet.proj.points.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PointTransactionRepository extends JpaRepository<PointTransactionEntity, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);

    List<PointTransactionEntity> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
}
