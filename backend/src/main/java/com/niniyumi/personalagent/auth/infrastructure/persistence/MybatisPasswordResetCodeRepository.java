package com.niniyumi.personalagent.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.niniyumi.personalagent.auth.domain.PasswordResetCode;
import com.niniyumi.personalagent.auth.domain.PasswordResetCodeRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisPasswordResetCodeRepository implements PasswordResetCodeRepository {
    private final PasswordResetCodeMapper mapper;

    public MybatisPasswordResetCodeRepository(PasswordResetCodeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PasswordResetCode save(PasswordResetCode code) {
        PasswordResetCodeRow row = PasswordResetCodeRow.from(code);
        mapper.insert(row);
        return row.toDomain();
    }

    @Override
    public Optional<PasswordResetCode> findLatestByUserId(long userId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<PasswordResetCodeRow>()
                .eq(PasswordResetCodeRow::getUserId, userId)
                .orderByDesc(PasswordResetCodeRow::getCreatedAt)
                .last("LIMIT 1"))).map(PasswordResetCodeRow::toDomain);
    }

    @Override
    public void deleteAllByUserId(long userId) {
        mapper.delete(new LambdaQueryWrapper<PasswordResetCodeRow>()
                .eq(PasswordResetCodeRow::getUserId, userId));
    }

    @Override
    public void markUsed(long id, Instant usedAt) {
        mapper.update(null, new LambdaUpdateWrapper<PasswordResetCodeRow>()
                .eq(PasswordResetCodeRow::getId, id)
                .set(PasswordResetCodeRow::getUsedAt, usedAt));
    }
}
