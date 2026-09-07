package com.niniyumi.personalagent.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.time.Instant;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MybatisPasswordResetCodeRepositoryTest {
    @Mock
    private PasswordResetCodeMapper mapper;

    @BeforeEach
    void initializeMybatisTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PasswordResetCodeRow.class);
    }

    @Test
    void markUsedIfValidUsesOneConditionalUpdate() {
        Instant now = Instant.parse("2026-08-25T12:00:00Z");
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        MybatisPasswordResetCodeRepository repository = new MybatisPasswordResetCodeRepository(mapper);

        assertThat(repository.markUsedIfValid(7L, now)).isTrue();

        ArgumentCaptor<LambdaUpdateWrapper<PasswordResetCodeRow>> update = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        org.mockito.Mockito.verify(mapper).update(isNull(), update.capture());
        assertThat(update.getValue().getSqlSet()).contains("used_at");
        assertThat(update.getValue().getSqlSegment()).contains("id", "used_at IS NULL", "expires_at >");
    }

    @Test
    void markUsedIfValidReportsFalseWhenAlreadyUsedOrExpired() {
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);
        MybatisPasswordResetCodeRepository repository = new MybatisPasswordResetCodeRepository(mapper);

        assertThat(repository.markUsedIfValid(7L, Instant.parse("2026-08-25T12:00:00Z"))).isFalse();
    }
}
