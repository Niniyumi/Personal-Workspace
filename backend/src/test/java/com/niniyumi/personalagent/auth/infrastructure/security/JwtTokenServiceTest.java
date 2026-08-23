package com.niniyumi.personalagent.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class JwtTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");
    private static final String SECRET = "test-jwt-secret-for-unit-tests-01";

    @Test
    void issuedTokenContainsUserIdentityAndFifteenMinuteExpiry() {
        JwtTokenService tokenService = new JwtTokenService(
                new JwtProperties(SECRET, 15), Clock.fixed(NOW, ZoneOffset.UTC));

        Jwt jwt = decoder().decode(tokenService.issueAccessToken(activeUser(42L)));

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("username")).isEqualTo("nini");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(Instant.parse("2026-08-23T08:15:00Z"));
    }

    private NimbusJwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey()).build();
        decoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
        return decoder;
    }

    private SecretKey secretKey() {
        return new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private User activeUser(long id) {
        return new User(id, "nini", "nini@example.com", "bcrypt-hash", "Nini",
                UserStatus.ACTIVE, null, null);
    }
}
