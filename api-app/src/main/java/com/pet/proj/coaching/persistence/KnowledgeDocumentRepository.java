package com.pet.proj.coaching.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocumentEntity, UUID> {
}
