package com.smartCommunity.SmartCommunity.users;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "app_users")
@Getter
@Setter

public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // Every new user starts as a normal USER (the column is NOT NULL, so it must have a value)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private boolean emailVerified = false;

    private String displayName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {
    }

    public AppUser(String email, String password) {
        this.email = email;
        this.password = password;
    }
}

