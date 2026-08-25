package com.niniyumi.personalagent.auth.infrastructure.security;

import com.niniyumi.personalagent.auth.domain.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        if (secret == null || secret.length() < 16) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 16 characters");
        }
        try {
            // 将便于本地配置的 16 位密钥派生为 HS256 要求的 32 字节密钥。
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "HmacSHA256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
