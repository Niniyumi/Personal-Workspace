package com.niniyumi.personalagent.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MybatisRefreshSessionRepositoryTest {
    @Mock
    private RefreshSessionMapper mapper;

    @BeforeEach
    void initializeMybatisTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RefreshSessionRow.class);
    }

    @Test
    void revokeIfActiveUsesSingleConditionalUpdateAndReportsTheAffectedRow() {
        Instant now = Instant.parse("2026-08-23T08:00:00Z");
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        MybatisRefreshSessionRepository repository = new MybatisRefreshSessionRepository(mapper);

        assertThat(repository.revokeIfActive(42L, now)).isTrue();

        ArgumentCaptor<LambdaUpdateWrapper<RefreshSessionRow>> update = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        org.mockito.Mockito.verify(mapper).update(isNull(), update.capture());
        assertThat(update.getValue().getSqlSet()).contains("revoked_at");
        assertThat(update.getValue().getSqlSegment()).contains("id", "revoked_at IS NULL", "expires_at >");
    }

    @Test
    void revokeIfActiveReportsFalseWhenTheSessionIsAlreadyConsumedOrExpired() {
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);
        MybatisRefreshSessionRepository repository = new MybatisRefreshSessionRepository(mapper);

        assertThat(repository.revokeIfActive(42L, Instant.parse("2026-08-23T08:00:00Z"))).isFalse();
    }
}
