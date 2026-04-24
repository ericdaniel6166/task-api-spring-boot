package com.eric6166.taskapi.controller;

import com.eric6166.taskapi.dto.task.TaskResponse;
import com.eric6166.taskapi.entity.TaskPriority;
import com.eric6166.taskapi.entity.TaskStatus;
import com.eric6166.taskapi.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    private TaskResponse sampleTaskResponse() {
        return new TaskResponse(
                1L, "Test Task", "description",
                TaskStatus.TODO, TaskPriority.MEDIUM,
                null,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    @Test
    @WithMockUser
    void listTasks_authenticated_returns200() throws Exception {
        var page = new PageImpl<>(List.of(sampleTaskResponse()), PageRequest.of(0, 20), 1);
        when(taskService.listTasks(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void createTask_validRequest_returns201() throws Exception {
        when(taskService.createTask(any())).thenReturn(sampleTaskResponse());

        String body = """
                {"title":"Test Task","description":"description"}
                """;

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    @WithMockUser
    void getTask_returns200() throws Exception {
        when(taskService.getTask(eq(1L))).thenReturn(sampleTaskResponse());

        mockMvc.perform(get("/api/v1/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @WithMockUser
    void deleteTask_returns204() throws Exception {
        doNothing().when(taskService).deleteTask(eq(1L));

        mockMvc.perform(delete("/api/v1/tasks/1"))
                .andExpect(status().isNoContent());
    }
}
