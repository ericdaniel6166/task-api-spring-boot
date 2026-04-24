package com.eric6166.taskapi.integration;

import com.eric6166.taskapi.dto.auth.AuthResponse;
import com.eric6166.taskapi.dto.auth.LoginRequest;
import com.eric6166.taskapi.dto.auth.RegisterRequest;
import com.eric6166.taskapi.dto.auth.RegisterResponse;
import com.eric6166.taskapi.dto.task.CreateTaskRequest;
import com.eric6166.taskapi.dto.task.TaskResponse;
import com.eric6166.taskapi.dto.task.UpdateTaskStatusRequest;
import com.eric6166.taskapi.entity.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TaskApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    // --- helpers ---

    private String uniqueEmail() {
        return UUID.randomUUID() + "@test.com";
    }

    private String registerAndLogin(String email) {
        // Register
        RegisterRequest registerRequest = new RegisterRequest(email, "password123");
        restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/register",
                registerRequest,
                RegisterResponse.class);

        // Login
        LoginRequest loginRequest = new LoginRequest(email, "password123");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/login",
                loginRequest,
                AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        return loginResponse.getBody().token();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // --- tests ---

    @Test
    void fullTaskCrudFlow() {
        String token = registerAndLogin(uniqueEmail());
        HttpHeaders headers = bearerHeaders(token);

        // Create task
        CreateTaskRequest createRequest = new CreateTaskRequest("Integration Task", "desc", null, null);
        ResponseEntity<TaskResponse> createResp = restTemplate.exchange(
                baseUrl + "/api/v1/tasks",
                HttpMethod.POST,
                new HttpEntity<>(createRequest, headers),
                TaskResponse.class);

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getBody()).isNotNull();
        Long taskId = createResp.getBody().id();
        assertThat(taskId).isNotNull();
        assertThat(createResp.getBody().title()).isEqualTo("Integration Task");

        // Get task
        ResponseEntity<TaskResponse> getResp = restTemplate.exchange(
                baseUrl + "/api/v1/tasks/" + taskId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                TaskResponse.class);

        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResp.getBody()).isNotNull();
        assertThat(getResp.getBody().id()).isEqualTo(taskId);

        // Patch status
        UpdateTaskStatusRequest statusRequest = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);
        ResponseEntity<TaskResponse> patchResp = restTemplate.exchange(
                baseUrl + "/api/v1/tasks/" + taskId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(statusRequest, headers),
                TaskResponse.class);

        assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResp.getBody()).isNotNull();
        assertThat(patchResp.getBody().status()).isEqualTo(TaskStatus.IN_PROGRESS);

        // Delete task
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                baseUrl + "/api/v1/tasks/" + taskId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class);

        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Confirm 404
        ResponseEntity<String> notFoundResp = restTemplate.exchange(
                baseUrl + "/api/v1/tasks/" + taskId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(notFoundResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void accessOtherUserTask_returns404() {
        // User 1 creates a task
        String token1 = registerAndLogin(uniqueEmail());
        HttpHeaders headers1 = bearerHeaders(token1);

        CreateTaskRequest createRequest = new CreateTaskRequest("User1 Task", null, null, null);
        ResponseEntity<TaskResponse> createResp = restTemplate.exchange(
                baseUrl + "/api/v1/tasks",
                HttpMethod.POST,
                new HttpEntity<>(createRequest, headers1),
                TaskResponse.class);

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long taskId = createResp.getBody().id();

        // User 2 tries to access user 1's task
        String token2 = registerAndLogin(uniqueEmail());
        HttpHeaders headers2 = bearerHeaders(token2);

        ResponseEntity<String> getResp = restTemplate.exchange(
                baseUrl + "/api/v1/tasks/" + taskId,
                HttpMethod.GET,
                new HttpEntity<>(headers2),
                String.class);

        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unauthenticated_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/v1/tasks",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
