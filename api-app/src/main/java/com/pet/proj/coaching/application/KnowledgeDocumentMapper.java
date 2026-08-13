package com.pet.proj.coaching.application;

import com.pet.proj.coaching.api.KnowledgeDocumentResponse;
import com.pet.proj.coaching.api.KnowledgeDocumentSummaryResponse;
import com.pet.proj.coaching.persistence.KnowledgeDocumentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KnowledgeDocumentMapper {
    KnowledgeDocumentResponse toResponse(KnowledgeDocumentEntity entity);

    KnowledgeDocumentSummaryResponse toSummaryResponse(KnowledgeDocumentEntity entity);
}
