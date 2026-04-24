package com.eric6166.taskapi.controller;

import com.eric6166.taskapi.dto.auth.AuthResponse;
import com.eric6166.taskapi.dto.auth.LoginRequest;
import com.eric6166.taskapi.dto.auth.RegisterRequest;
import com.eric6166.taskapi.dto.auth.RegisterResponse;
import com.eric6166.taskapi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
