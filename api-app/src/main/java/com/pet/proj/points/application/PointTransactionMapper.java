package com.pet.proj.points.application;

import com.pet.proj.points.persistence.PointTransactionEntity;
import com.pet.proj.points.persistence.PointTransactionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface PointTransactionMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    PointTransactionEntity toEntity(UUID userId, PointTransactionType type, int amount,
                                    UUID taskId, UUID submissionId, String idempotencyKey);
}
