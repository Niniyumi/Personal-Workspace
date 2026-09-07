package com.niniyumi.personalagent.auth.domain;

import java.time.Instant;

public record RegistrationVerificationCode(
        String email,
        String code,
        Instant expiresAt,
        Instant createdAt) {
}
