package com.niniyumi.personalagent.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.niniyumi.personalagent.auth.domain.RefreshSession;
import com.niniyumi.personalagent.auth.domain.RefreshSessionRepository;
import com.niniyumi.personalagent.auth.infrastructure.security.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");

    @Mock
    private RefreshSessionRepository repository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, Clock.fixed(NOW, ZoneOffset.UTC),
                new JwtProperties("unused", 15, 9));
    }

    @Test
    void issueStoresOnlySha256HashAndReturnsOpaqueToken() {
        RefreshSession persistedSession = new RefreshSession(7L, 42L, "persisted-hash",
                NOW.plus(9, ChronoUnit.DAYS), null, NOW);
        when(repository.save(org.mockito.ArgumentMatchers.any(RefreshSession.class))).thenReturn(persistedSession);

        IssuedRefreshToken issued = service.issue(42L);
        ArgumentCaptor<RefreshSession> session = ArgumentCaptor.forClass(RefreshSession.class);
        verify(repository).save(session.capture());

        assertThat(issued.value()).matches("[A-Za-z0-9_-]{43}");
        assertThat(session.getValue().tokenHash()).isNotEqualTo(issued.value());
        assertThat(session.getValue().tokenHash()).matches("[0-9a-f]{64}");
        assertThat(session.getValue().expiresAt()).isEqualTo(NOW.plus(9, ChronoUnit.DAYS));
        assertThat(issued.session()).isEqualTo(persistedSession);
    }

    @Test
    void consumeRejectsRevokedSession() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(
                new RefreshSession(1L, 42L, "hash", NOW.plus(7, ChronoUnit.DAYS), NOW, NOW)));

        assertThatThrownBy(() -> service.consume("opaque-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void consumeRejectsExpiredSession() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(
                new RefreshSession(1L, 42L, "hash", NOW, null, NOW.minus(7, ChronoUnit.DAYS))));

        assertThatThrownBy(() -> service.consume("opaque-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void consumeRejectsSessionWhenAnotherRequestAlreadyConsumedIt() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(
                new RefreshSession(1L, 42L, "hash", NOW.plus(7, ChronoUnit.DAYS), null, NOW)));
        when(repository.revokeIfActive(1L, NOW)).thenReturn(false);

        assertThatThrownBy(() -> service.consume("opaque-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void consumeReturnsSessionAfterAtomicallyRevokingIt() {
        RefreshSession session = new RefreshSession(1L, 42L, "hash", NOW.plus(7, ChronoUnit.DAYS), null, NOW);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(session));
        when(repository.revokeIfActive(1L, NOW)).thenReturn(true);

        assertThat(service.consume("opaque-token")).isEqualTo(session);
    }
}
