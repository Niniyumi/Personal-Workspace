package com.niniyumi.personalagent.auth.infrastructure.security;

import com.niniyumi.personalagent.auth.domain.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtProperties properties;
    private final Clock clock;
    private final JwtEncoder encoder;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey(properties.jwtSecret())));
    }

    public String issueAccessToken(User user) {
        Instant issuedAt = clock.instant();
        // sub 只保存稳定的用户 ID；用户名仅作为展示性声明，不作为数据库主键。
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .claim("username", user.username())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(properties.accessTokenMinutes() * 60))
                .build();
        return encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    static SecretKey secretKey(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
