package com.pet.proj.submission.application;

import com.pet.proj.submission.domain.Submission;
import com.pet.proj.submission.persistence.SubmissionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubmissionMapper {
    Submission toDomain(SubmissionEntity entity);
}
