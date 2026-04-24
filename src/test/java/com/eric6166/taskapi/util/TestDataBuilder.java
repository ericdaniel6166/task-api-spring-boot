package com.eric6166.taskapi.util;

import com.eric6166.taskapi.entity.Task;
import com.eric6166.taskapi.entity.TaskPriority;
import com.eric6166.taskapi.entity.TaskStatus;
import com.eric6166.taskapi.entity.User;

import java.time.OffsetDateTime;

public final class TestDataBuilder {

    private TestDataBuilder() {
    }

    public static User aUser(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .password("$2a$10$hashedpassword")
                .role("ROLE_USER")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    public static Task aTask(Long id, User user) {
        return Task.builder()
                .id(id)
                .title("Test Task " + id)
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .user(user)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
