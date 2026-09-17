package com.niniyumi.personalagent.course.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.niniyumi.personalagent.course.domain.CourseAudioPart;
import com.niniyumi.personalagent.course.domain.CourseAudioPartRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisCourseAudioPartRepository implements CourseAudioPartRepository {
    private final CourseAudioPartMapper mapper;

    public MybatisCourseAudioPartRepository(CourseAudioPartMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public CourseAudioPart save(CourseAudioPart part) {
        CourseAudioPartRow row = CourseAudioPartRow.fromDomain(part);
        mapper.insert(row);
        return row.toDomain();
    }

    @Override
    public CourseAudioPart update(CourseAudioPart part) {
        CourseAudioPartRow row = CourseAudioPartRow.fromDomain(part);
        mapper.updateById(row);
        return row.toDomain();
    }

    @Override
    public Optional<CourseAudioPart> findByCourseIdAndPartNumber(long courseId, int partNumber) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<CourseAudioPartRow>()
                        .eq(CourseAudioPartRow::getCourseId, courseId)
                        .eq(CourseAudioPartRow::getPartNumber, partNumber)))
                .map(CourseAudioPartRow::toDomain);
    }

    @Override
    public List<CourseAudioPart> findAllByCourseId(long courseId) {
        return mapper.selectList(new LambdaQueryWrapper<CourseAudioPartRow>()
                        .eq(CourseAudioPartRow::getCourseId, courseId)
                        .orderByAsc(CourseAudioPartRow::getPartNumber))
                .stream().map(CourseAudioPartRow::toDomain).toList();
    }

    @Override
    public void deleteAllByCourseId(long courseId) {
        mapper.delete(new LambdaQueryWrapper<CourseAudioPartRow>()
                .eq(CourseAudioPartRow::getCourseId, courseId));
    }
}
