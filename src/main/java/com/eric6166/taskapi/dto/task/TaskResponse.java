package com.eric6166.taskapi.dto.task;

import com.eric6166.taskapi.entity.TaskPriority;
import com.eric6166.taskapi.entity.TaskStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
