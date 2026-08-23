package com.niniyumi.personalagent.auth.application;

import com.niniyumi.personalagent.auth.domain.RefreshSession;
import com.niniyumi.personalagent.auth.domain.RefreshSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshSessionRepository repository;
    private final Clock clock;

    public RefreshTokenService(RefreshSessionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public IssuedRefreshToken issue(long userId) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        RefreshSession session = new RefreshSession(null, userId, hash(token), now.plus(7, ChronoUnit.DAYS), null, now);
        repository.save(session);
        return new IssuedRefreshToken(token, session);
    }

    public RefreshSession validate(String token) {
        RefreshSession session = repository.findByTokenHash(hash(token))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!session.isActive(clock.instant())) {
            throw new InvalidRefreshTokenException();
        }
        return session;
    }

    public void revoke(RefreshSession session) {
        repository.revoke(session.id(), clock.instant());
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
