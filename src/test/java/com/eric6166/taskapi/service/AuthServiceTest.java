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
import com.eric6166.taskapi.util.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_success() {
        // Arrange
        RegisterRequest request = new RegisterRequest("user@test.com", "password123");
        User savedUser = TestDataBuilder.aUser(1L, "user@test.com");

        when(userRepository.existsByUsername("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        RegisterResponse response = authService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("user@test.com");
        assertThat(response.createdAt()).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        // Arrange
        RegisterRequest request = new RegisterRequest("existing@test.com", "password123");
        when(userRepository.existsByUsername("existing@test.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("existing@test.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        // Arrange
        LoginRequest request = new LoginRequest("user@test.com", "password123");
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("user@test.com")
                .password("$2a$10$hashed")
                .roles("USER")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt.token.here");
        when(appProperties.getExpirationMs()).thenReturn(86400000L);

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt.token.here");
        assertThat(response.expiresIn()).isEqualTo(86400000L);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(userDetails);
    }
}
