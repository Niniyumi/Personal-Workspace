package com.niniyumi.personalagent.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.niniyumi.personalagent.auth.domain.PasswordResetCode;
import java.time.Instant;

@TableName("password_reset_codes")
public class PasswordResetCodeRow {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String codeHash;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt;

    static PasswordResetCodeRow from(PasswordResetCode code) {
        PasswordResetCodeRow row = new PasswordResetCodeRow();
        row.id = code.id(); row.userId = code.userId(); row.codeHash = code.codeHash();
        row.expiresAt = code.expiresAt(); row.usedAt = code.usedAt(); row.createdAt = code.createdAt();
        return row;
    }

    PasswordResetCode toDomain() {
        return new PasswordResetCode(id, userId, codeHash, expiresAt, usedAt, createdAt);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
