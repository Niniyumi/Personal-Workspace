package com.niniyumi.personalagent.auth.domain;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetCodeRepository {
    PasswordResetCode save(PasswordResetCode code);
    Optional<PasswordResetCode> findLatestByUserId(long userId);
    void deleteAllByUserId(long userId);
    void markUsed(long id, Instant usedAt);
}
