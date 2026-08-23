package com.niniyumi.personalagent.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import java.time.Instant;

@TableName("users")
public class UserRow {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String displayName;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserRow fromDomain(User user) {
        UserRow row = new UserRow();
        row.id = user.id();
        row.username = user.username();
        row.email = user.email();
        row.passwordHash = user.passwordHash();
        row.displayName = user.displayName();
        row.status = user.status().name();
        row.createdAt = user.createdAt();
        row.updatedAt = user.updatedAt();
        return row;
    }

    public User toDomain() {
        return new User(id, username, email, passwordHash, displayName,
                UserStatus.valueOf(status), createdAt, updatedAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
