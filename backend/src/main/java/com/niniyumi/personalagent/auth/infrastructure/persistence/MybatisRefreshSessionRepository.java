package com.niniyumi.personalagent.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.niniyumi.personalagent.auth.domain.RefreshSession;
import com.niniyumi.personalagent.auth.domain.RefreshSessionRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisRefreshSessionRepository implements RefreshSessionRepository {
    private final RefreshSessionMapper refreshSessionMapper;

    public MybatisRefreshSessionRepository(RefreshSessionMapper refreshSessionMapper) {
        this.refreshSessionMapper = refreshSessionMapper;
    }

    @Override
    public RefreshSession save(RefreshSession session) {
        RefreshSessionRow row = RefreshSessionRow.fromDomain(session);
        refreshSessionMapper.insert(row);
        return row.toDomain();
    }

    @Override
    public Optional<RefreshSession> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(refreshSessionMapper.selectOne(new LambdaQueryWrapper<RefreshSessionRow>()
                .eq(RefreshSessionRow::getTokenHash, tokenHash)))
                .map(RefreshSessionRow::toDomain);
    }

    @Override
    public boolean revokeIfActive(long id, Instant revokedAt) {
        // 在一条 SQL 中同时校验未撤销和未过期，避免并发请求重复消费令牌。
        return refreshSessionMapper.update(null, new LambdaUpdateWrapper<RefreshSessionRow>()
                .eq(RefreshSessionRow::getId, id)
                .isNull(RefreshSessionRow::getRevokedAt)
                .gt(RefreshSessionRow::getExpiresAt, revokedAt)
                .set(RefreshSessionRow::getRevokedAt, revokedAt)) == 1;
    }

    @Override
    public int revokeAllActiveByUserId(long userId, Instant revokedAt) {
        return refreshSessionMapper.update(null, new LambdaUpdateWrapper<RefreshSessionRow>()
                .eq(RefreshSessionRow::getUserId, userId)
                .isNull(RefreshSessionRow::getRevokedAt)
                .gt(RefreshSessionRow::getExpiresAt, revokedAt)
                .set(RefreshSessionRow::getRevokedAt, revokedAt));
    }
}
