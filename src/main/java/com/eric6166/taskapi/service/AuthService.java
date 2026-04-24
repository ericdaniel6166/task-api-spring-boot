package com.eric6166.taskapi.service;

import com.eric6166.taskapi.config.AppProperties;
import com.eric6166.taskapi.dto.auth.AuthResponse;
import com.eric6166.taskapi.dto.auth.LoginRequest;
import com.eric6166.taskapi.dto.auth.RegisterRequest;
import com.eric6166.taskapi.dto.auth.RegisterResponse;
import com.eric6166.taskapi.entity.User;
import com.eric6166.taskapi.exception.ConflictException;
import com.eric6166.taskapi.repository.UserRepository;
import com.eric6166.taskapi.security.JwtService;
import com.eric6166.taskapi.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final AppProperties appProperties;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already taken: " + request.username());
        }
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role("ROLE_USER")
                .build();
        user = userRepository.save(user);
        return new RegisterResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, appProperties.getExpirationMs());
    }
}
