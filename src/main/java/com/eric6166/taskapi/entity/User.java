package com.eric6166.taskapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Task> tasks;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
