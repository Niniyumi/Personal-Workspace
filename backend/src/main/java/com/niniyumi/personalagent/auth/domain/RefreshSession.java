package com.niniyumi.personalagent.auth.domain;

import java.time.Instant;

public record RefreshSession(
        Long id,
        long userId,
        String tokenHash,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt) {

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }
}
