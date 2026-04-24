package com.eric6166.taskapi.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String username,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}
