package com.niniyumi.personalagent.course.domain;

import java.util.List;
import java.util.Optional;

public interface CourseAudioPartRepository {
    CourseAudioPart save(CourseAudioPart part);

    default CourseAudioPart update(CourseAudioPart part) {
        throw new UnsupportedOperationException("Part transcript update is not implemented");
    }

    Optional<CourseAudioPart> findByCourseIdAndPartNumber(long courseId, int partNumber);

    List<CourseAudioPart> findAllByCourseId(long courseId);

    void deleteAllByCourseId(long courseId);
}
