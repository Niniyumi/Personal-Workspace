package com.niniyumi.personalagent.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MybatisUserRepositoryTest {
    @Mock
    private UserMapper mapper;

    private MybatisUserRepository repository;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), UserRow.class);
        repository = new MybatisUserRepository(mapper);
    }

    @Test
    void usernameLoginQueriesOnlyUsernameColumn() {
        repository.findByUsernameOrEmail("nini");

        LambdaQueryWrapper<UserRow> query = capturedQuery();
        assertThat(query.getSqlSegment()).contains("username").doesNotContain("email", " OR ");
    }

    @Test
    void emailLoginQueriesOnlyEmailColumn() {
        repository.findByUsernameOrEmail("nini@example.com");

        LambdaQueryWrapper<UserRow> query = capturedQuery();
        assertThat(query.getSqlSegment()).contains("email").doesNotContain("username", " OR ");
    }

    private LambdaQueryWrapper<UserRow> capturedQuery() {
        ArgumentCaptor<LambdaQueryWrapper<UserRow>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        org.mockito.Mockito.verify(mapper).selectOne(query.capture());
        return query.getValue();
    }
}
