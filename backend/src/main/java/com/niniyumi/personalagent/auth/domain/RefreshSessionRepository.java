package com.niniyumi.personalagent.auth.domain;

import java.time.Instant;
import java.util.Optional;

public interface RefreshSessionRepository {
    RefreshSession save(RefreshSession session);

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    void revoke(long id, Instant revokedAt);
}
