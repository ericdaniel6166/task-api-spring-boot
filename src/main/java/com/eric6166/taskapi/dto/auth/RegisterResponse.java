package com.eric6166.taskapi.dto.auth;

import java.time.OffsetDateTime;

public record RegisterResponse(Long id, String username, OffsetDateTime createdAt) {
}
