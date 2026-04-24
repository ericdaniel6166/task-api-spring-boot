package com.eric6166.taskapi.dto.task;

import com.eric6166.taskapi.entity.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        TaskPriority priority,
        @FutureOrPresent LocalDate dueDate
) {
}
