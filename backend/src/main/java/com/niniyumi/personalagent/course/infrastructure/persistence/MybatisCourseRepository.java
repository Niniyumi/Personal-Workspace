package com.niniyumi.personalagent.course.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisCourseRepository implements CourseRepository {
    private final CourseMapper mapper;

    public MybatisCourseRepository(CourseMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Course save(Course course) {
        CourseRow row = CourseRow.fromDomain(course);
        mapper.insert(row);
        return row.toDomain();
    }

    @Override
    public Course update(Course course) {
        mapper.update(null, new LambdaUpdateWrapper<CourseRow>()
                .eq(CourseRow::getId, course.id())
                .eq(CourseRow::getUserId, course.userId())
                .set(CourseRow::getStatus, course.status())
                .set(CourseRow::getDurationSeconds, course.durationSeconds())
                .set(CourseRow::getProcessingProgress, course.processingProgress())
                .set(CourseRow::getTranscript, course.transcript())
                .set(CourseRow::getNoteContent, course.noteContent())
                .set(CourseRow::getErrorMessage, course.errorMessage())
                .set(CourseRow::getSourceType, course.sourceType())
                .set(CourseRow::getOriginalAudioPath, course.originalAudioPath())
                .set(CourseRow::getExpectedBytes, course.expectedBytes())
                .set(CourseRow::getNoteCandidate, course.noteCandidate())
                .set(CourseRow::getUpdatedAt, course.updatedAt()));
        return course;
    }

    @Override
    public Course updateProgress(Course course) {
        mapper.update(null, new LambdaUpdateWrapper<CourseRow>()
                .eq(CourseRow::getId, course.id())
                .eq(CourseRow::getUserId, course.userId())
                .set(CourseRow::getStatus, course.status())
                .set(CourseRow::getProcessingProgress, course.processingProgress())
                .set(CourseRow::getErrorMessage, course.errorMessage())
                .set(CourseRow::getUpdatedAt, course.updatedAt()));
        return course;
    }

    @Override
    public boolean updateTitle(long id, long userId, String title, java.time.Instant updatedAt) {
        return mapper.update(null, new LambdaUpdateWrapper<CourseRow>()
                .eq(CourseRow::getId, id)
                .eq(CourseRow::getUserId, userId)
                .set(CourseRow::getTitle, title)
                .set(CourseRow::getUpdatedAt, updatedAt)) > 0;
    }

    @Override
    public Optional<Course> findByIdAndUserId(long id, long userId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<CourseRow>()
                        .eq(CourseRow::getId, id)
                        .eq(CourseRow::getUserId, userId)))
                .map(CourseRow::toDomain);
    }

    @Override
    public Optional<CourseProgress> findProgressByIdAndUserId(long id, long userId) {
        CourseRow row = mapper.selectOne(new LambdaQueryWrapper<CourseRow>()
                .select(CourseRow::getStatus, CourseRow::getProcessingProgress, CourseRow::getErrorMessage)
                .eq(CourseRow::getId, id)
                .eq(CourseRow::getUserId, userId));
        return Optional.ofNullable(row).map(value -> new CourseProgress(
                value.getStatus(), value.getProcessingProgress(), value.getErrorMessage()));
    }

    @Override
    public List<Course> findAllByUserId(long userId) {
        return mapper.selectList(new QueryWrapper<CourseRow>()
                        .select("id", "user_id", "title", "course_name", "lesson_date", "status", "duration_seconds",
                                "processing_progress", "error_message", "source_type", "created_at", "updated_at",
                                "CASE WHEN note_content IS NULL OR note_content = '' THEN NULL ELSE '1' END AS note_content")
                        .eq("user_id", userId)
                        .orderByDesc("created_at"))
                .stream().map(CourseRow::toDomain).toList();
    }
}
