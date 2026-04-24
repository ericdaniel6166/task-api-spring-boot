package com.eric6166.taskapi.service;

import com.eric6166.taskapi.dto.task.CreateTaskRequest;
import com.eric6166.taskapi.dto.task.TaskResponse;
import com.eric6166.taskapi.dto.task.UpdateTaskRequest;
import com.eric6166.taskapi.dto.task.UpdateTaskStatusRequest;
import com.eric6166.taskapi.entity.Task;
import com.eric6166.taskapi.entity.TaskPriority;
import com.eric6166.taskapi.entity.TaskStatus;
import com.eric6166.taskapi.exception.ResourceNotFoundException;
import com.eric6166.taskapi.repository.TaskRepository;
import com.eric6166.taskapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private Long currentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"))
                .getId();
    }

    private Task findOwnedTask(Long taskId, Long userId) {
        return taskRepository.findById(taskId)
                .filter(t -> t.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(), task.getTitle(), task.getDescription(),
                task.getStatus(), task.getPriority(), task.getDueDate(),
                task.getCreatedAt(), task.getUpdatedAt()
        );
    }

    public Page<TaskResponse> listTasks(TaskStatus status, Pageable pageable) {
        Long userId = currentUserId();
        Page<Task> page = (status != null)
                ? taskRepository.findByUserIdAndStatus(userId, status, pageable)
                : taskRepository.findByUserId(userId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        Long userId = currentUserId();
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var task = Task.builder()
                .title(request.title())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .dueDate(request.dueDate())
                .user(user)
                .build();
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse getTask(Long id) {
        return toResponse(findOwnedTask(id, currentUserId()));
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = findOwnedTask(id, currentUserId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.priority() != null) task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = findOwnedTask(id, currentUserId());
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long id, UpdateTaskStatusRequest request) {
        Task task = findOwnedTask(id, currentUserId());
        task.setStatus(request.status());
        return toResponse(taskRepository.save(task));
    }
}
