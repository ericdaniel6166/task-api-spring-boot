package com.eric6166.taskapi.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
