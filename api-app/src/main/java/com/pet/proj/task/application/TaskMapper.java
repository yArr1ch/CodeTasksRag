package com.pet.proj.task.application;

import com.pet.proj.task.domain.Task;
import com.pet.proj.task.persistence.TaskEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = TaskJsonMapper.class)
public interface TaskMapper {

    @Mapping(target = "constraints", source = "constraints", qualifiedByName = "strings")
    @Mapping(target = "testCases", source = "testCases", qualifiedByName = "testCases")
    Task toDomain(TaskEntity entity);

    @Mapping(target = "constraints", source = "constraints", qualifiedByName = "text")
    @Mapping(target = "testCases", source = "testCases", qualifiedByName = "text")
    @Mapping(target = "concepts", source = "concepts", qualifiedByName = "text")
    TaskReviewPrompt toReviewPrompt(TaskEntity entity);

    @Mapping(target = "title", source = "entity.title")
    @Mapping(target = "description", source = "entity.description")
    @Mapping(target = "constraints", source = "entity.constraints", qualifiedByName = "text")
    @Mapping(target = "examples", source = "entity.testCases", qualifiedByName = "text")
    @Mapping(target = "concepts", source = "entity.concepts", qualifiedByName = "text")
    @Mapping(target = "level", source = "level")
    @Mapping(target = "knowledgeContext", source = "knowledgeContext")
    TaskHintPrompt toHintPrompt(TaskEntity entity, int level, String knowledgeContext);
}
