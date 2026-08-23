package com.niniyumi.personalagent.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.niniyumi.personalagent.auth.domain.RefreshSession;
import java.time.Instant;

@TableName("refresh_sessions")
public class RefreshSessionRow {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private long userId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant revokedAt;
    private Instant createdAt;

    public static RefreshSessionRow fromDomain(RefreshSession session) {
        RefreshSessionRow row = new RefreshSessionRow();
        row.id = session.id();
        row.userId = session.userId();
        row.tokenHash = session.tokenHash();
        row.expiresAt = session.expiresAt();
        row.revokedAt = session.revokedAt();
        row.createdAt = session.createdAt();
        return row;
    }

    public RefreshSession toDomain() {
        return new RefreshSession(id, userId, tokenHash, expiresAt, revokedAt, createdAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
