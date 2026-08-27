package com.pet.proj.task.api;

import com.pet.proj.task.domain.Task;

import java.util.List;
import java.util.UUID;

public record TaskPageResponse(
        List<Task> tasks,
        UUID nextCursor,
        boolean hasMore
) {
}
