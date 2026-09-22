package com.niniyumi.personalagent.course.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MybatisCourseRepositoryTest {
    @Test
    void listQueryDoesNotSelectLargeCourseContentColumns() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "course-test"), CourseRow.class);
        CourseMapper mapper = mock(CourseMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        MybatisCourseRepository repository = new MybatisCourseRepository(mapper);

        repository.findAllByUserId(42L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<CourseRow>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        org.mockito.Mockito.verify(mapper).selectList(captor.capture());
        String selected = captor.getValue().getSqlSelect();
        assertThat(selected).isNotBlank()
                .contains("CASE WHEN note_content")
                .doesNotContain("transcript", "note_candidate", "original_audio_path");
    }

    @Test
    void processingUpdatesNeverOverwriteARenamedTitle() {
        initTableInfo();
        CourseMapper mapper = mock(CourseMapper.class);
        MybatisCourseRepository repository = new MybatisCourseRepository(mapper);
        Course course = course("任务开始时的旧标题");

        repository.update(course);

        ArgumentCaptor<CourseRow> rowCaptor = ArgumentCaptor.forClass(CourseRow.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<CourseRow>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).update(rowCaptor.capture(), wrapperCaptor.capture());
        assertThat(rowCaptor.getValue()).isNull();
        assertThat(wrapperCaptor.getValue().getSqlSet()).doesNotContain("title");
    }

    @Test
    void renameUpdatesOnlyTheOwnedTitle() {
        initTableInfo();
        CourseMapper mapper = mock(CourseMapper.class);
        when(mapper.update(any(), any())).thenReturn(1);
        MybatisCourseRepository repository = new MybatisCourseRepository(mapper);

        assertThat(repository.updateTitle(9L, 42L, "新笔记名称", Instant.parse("2026-09-21T08:00:00Z")))
                .isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<CourseRow>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).update(org.mockito.ArgumentMatchers.isNull(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSet())
                .contains("title")
                .doesNotContain("transcript", "note_content", "note_candidate");
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("id", "user_id");
    }

    private void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "course-update-test"), CourseRow.class);
    }

    private Course course(String title) {
        Instant time = Instant.parse("2026-09-21T08:00:00Z");
        return new Course(9L, 42L, title, CourseStatus.READY, 300, 100,
                "长转写", "长笔记", null, "IMPORT", "audio/source.m4a", 100L,
                "候选笔记", time, time);
    }
}
