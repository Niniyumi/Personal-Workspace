package com.niniyumi.personalagent.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class JwtTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-23T08:00:00Z");
    private static final String SECRET = "test-jwt-secret-for-unit-tests-01";
    private static final String SIXTEEN_CHARACTER_SECRET = "1234567890abcdef";

    @Test
    void issuedTokenContainsUserIdentityAndFifteenMinuteExpiry() {
        JwtTokenService tokenService = new JwtTokenService(
                new JwtProperties(SECRET, 15, 7), Clock.fixed(NOW, ZoneOffset.UTC));

        Jwt jwt = decoder().decode(tokenService.issueAccessToken(activeUser(42L)));

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("username")).isEqualTo("nini");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(Instant.parse("2026-08-23T08:15:00Z"));
    }

    @Test
    void sixteenCharacterSecretCanIssueAndVerifyToken() {
        JwtTokenService tokenService = new JwtTokenService(
                new JwtProperties(SIXTEEN_CHARACTER_SECRET, 15, 7), Clock.fixed(NOW, ZoneOffset.UTC));

        String token = tokenService.issueAccessToken(activeUser(42L));
        Jwt jwt = decoder(SIXTEEN_CHARACTER_SECRET).decode(token);

        assertThat(jwt.getSubject()).isEqualTo("42");
    }

    @Test
    void secretShorterThanSixteenCharactersIsRejected() {
        assertThatThrownBy(() -> JwtTokenService.secretKey("1234567890abcde"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("16");
    }

    private NimbusJwtDecoder decoder() {
        return decoder(SECRET);
    }

    private NimbusJwtDecoder decoder(String secret) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(secret)).build();
        decoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
        return decoder;
    }

    private SecretKey secretKey(String secret) {
        return JwtTokenService.secretKey(secret);
    }

    private User activeUser(long id) {
        return new User(id, "nini", "nini@example.com", "bcrypt-hash", "Nini",
                UserStatus.ACTIVE, null, null);
    }
}
