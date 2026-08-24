package com.niniyumi.personalagent.course.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseRepository;
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
        CourseRow row = CourseRow.fromDomain(course);
        mapper.update(row, new LambdaUpdateWrapper<CourseRow>()
                .eq(CourseRow::getId, course.id())
                .eq(CourseRow::getUserId, course.userId()));
        return row.toDomain();
    }

    @Override
    public Optional<Course> findByIdAndUserId(long id, long userId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<CourseRow>()
                        .eq(CourseRow::getId, id)
                        .eq(CourseRow::getUserId, userId)))
                .map(CourseRow::toDomain);
    }

    @Override
    public List<Course> findAllByUserId(long userId) {
        return mapper.selectList(new LambdaQueryWrapper<CourseRow>()
                        .eq(CourseRow::getUserId, userId)
                        .orderByDesc(CourseRow::getCreatedAt))
                .stream().map(CourseRow::toDomain).toList();
    }
}
