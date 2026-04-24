package com.eric6166.taskapi.dto.auth;

public record AuthResponse(String token, long expiresIn) {
}
