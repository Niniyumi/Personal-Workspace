package com.niniyumi.personalagent.auth.api.dto;

public record AuthTokensResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {
}
