package com.eric6166.taskapi.dto.task;

import com.eric6166.taskapi.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {
}
