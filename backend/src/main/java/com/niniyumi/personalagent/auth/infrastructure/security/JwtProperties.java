package com.niniyumi.personalagent.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record JwtProperties(String jwtSecret, long accessTokenMinutes, long refreshTokenDays) {
}
