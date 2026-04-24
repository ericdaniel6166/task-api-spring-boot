package com.eric6166.taskapi.service;

import com.eric6166.taskapi.dto.task.CreateTaskRequest;
import com.eric6166.taskapi.dto.task.TaskResponse;
import com.eric6166.taskapi.entity.Task;
import com.eric6166.taskapi.entity.TaskStatus;
import com.eric6166.taskapi.entity.User;
import com.eric6166.taskapi.exception.ResourceNotFoundException;
import com.eric6166.taskapi.repository.TaskRepository;
import com.eric6166.taskapi.repository.UserRepository;
import com.eric6166.taskapi.util.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final String TEST_USERNAME = "user@test.com";
    private static final Long USER_ID = 1L;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private TaskService taskService;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Set up SecurityContextHolder with a mock authenticated user
        var auth = new UsernamePasswordAuthenticationToken(
                TEST_USERNAME, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        testUser = TestDataBuilder.aUser(USER_ID, TEST_USERNAME);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTask_success() {
        // Arrange
        CreateTaskRequest request = new CreateTaskRequest("My Task", "desc", null, null);
        Task savedTask = TestDataBuilder.aTask(10L, testUser);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // Act
        TaskResponse response = taskService.createTask(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("Test Task 10");
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void getTask_notOwned_throwsNotFound() {
        // Arrange
        User otherUser = TestDataBuilder.aUser(99L, "other@test.com");
        Task otherTask = TestDataBuilder.aTask(5L, otherUser);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(taskRepository.findById(5L)).thenReturn(Optional.of(otherTask));

        // Act & Assert
        assertThatThrownBy(() -> taskService.getTask(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("5");
    }

    @Test
    void deleteTask_success() {
        // Arrange
        Task ownedTask = TestDataBuilder.aTask(7L, testUser);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(taskRepository.findById(7L)).thenReturn(Optional.of(ownedTask));

        // Act
        taskService.deleteTask(7L);

        // Assert
        verify(taskRepository).delete(ownedTask);
    }

    @Test
    void listTasks_noStatusFilter_returnsPage() {
        // Arrange
        Task task1 = TestDataBuilder.aTask(1L, testUser);
        Task task2 = TestDataBuilder.aTask(2L, testUser);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> taskPage = new PageImpl<>(List.of(task1, task2), pageable, 2);

        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(taskRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(taskPage);

        // Act
        Page<TaskResponse> result = taskService.listTasks(null, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        verify(taskRepository).findByUserId(eq(USER_ID), any(Pageable.class));
        verify(taskRepository, never()).findByUserIdAndStatus(any(), any(TaskStatus.class), any(Pageable.class));
    }
}
