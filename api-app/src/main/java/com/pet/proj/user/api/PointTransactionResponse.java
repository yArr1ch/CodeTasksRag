package com.pet.proj.user.api;

import com.pet.proj.points.persistence.PointTransactionType;

import java.time.Instant;
import java.util.UUID;

public record PointTransactionResponse(PointTransactionType type, int amount, UUID taskId,
                                       UUID submissionId, Instant createdAt) {
}
