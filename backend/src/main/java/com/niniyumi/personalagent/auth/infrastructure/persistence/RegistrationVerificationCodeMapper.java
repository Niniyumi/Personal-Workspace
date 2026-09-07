package com.niniyumi.personalagent.auth.infrastructure.persistence;

import java.time.Instant;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegistrationVerificationCodeMapper {
    @Insert("""
            INSERT INTO registration_verification_codes (email, code, expires_at, created_at)
            VALUES (#{email}, #{code}, #{expiresAt}, #{createdAt})
            ON DUPLICATE KEY UPDATE
                code = VALUES(code), expires_at = VALUES(expires_at), created_at = VALUES(created_at)
            """)
    int replace(@Param("email") String email, @Param("code") String code,
            @Param("expiresAt") Instant expiresAt, @Param("createdAt") Instant createdAt);

    @Delete("""
            DELETE FROM registration_verification_codes
            WHERE email = #{email} AND code = #{code} AND expires_at > #{now}
            """)
    int consume(@Param("email") String email, @Param("code") String code,
            @Param("now") Instant now);
}
