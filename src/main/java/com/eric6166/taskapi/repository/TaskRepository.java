package com.eric6166.taskapi.repository;

import com.eric6166.taskapi.entity.Task;
import com.eric6166.taskapi.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByUserId(Long userId, Pageable pageable);

    Page<Task> findByUserIdAndStatus(Long userId, TaskStatus status, Pageable pageable);

    boolean existsByIdAndUserId(Long id, Long userId);
}
