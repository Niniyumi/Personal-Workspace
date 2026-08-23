package com.niniyumi.personalagent.auth.domain;

import java.time.Instant;

public record User(
        Long id,
        String username,
        String email,
        String passwordHash,
        String displayName,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
