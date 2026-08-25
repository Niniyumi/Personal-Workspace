package com.niniyumi.personalagent.auth.domain;

import java.time.Instant;

public record PasswordResetCode(
        Long id, long userId, String codeHash, Instant expiresAt, Instant usedAt, Instant createdAt) {
    public boolean isValidAt(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
