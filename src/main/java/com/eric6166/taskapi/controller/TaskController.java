package com.eric6166.taskapi.controller;

import com.eric6166.taskapi.dto.task.CreateTaskRequest;
import com.eric6166.taskapi.dto.task.TaskResponse;
import com.eric6166.taskapi.dto.task.UpdateTaskRequest;
import com.eric6166.taskapi.dto.task.UpdateTaskStatusRequest;
import com.eric6166.taskapi.entity.TaskStatus;
import com.eric6166.taskapi.service.TaskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public Page<TaskResponse> listTasks(
            @RequestParam(required = false) TaskStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return taskService.listTasks(status, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.createTask(request);
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable Long id) {
        return taskService.getTask(id);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id,
                                   @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateTaskStatus(@PathVariable Long id,
                                         @Valid @RequestBody UpdateTaskStatusRequest request) {
        return taskService.updateTaskStatus(id, request);
    }
}
