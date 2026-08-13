package com.pet.proj.coaching.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocumentEntity, UUID> {
    List<KnowledgeDocumentEntity> findAllByOrderByIdAsc(Pageable pageable);
    List<KnowledgeDocumentEntity> findByIdGreaterThanOrderByIdAsc(UUID cursor, Pageable pageable);
}
