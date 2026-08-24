package com.niniyumi.personalagent.course.domain;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    Course save(Course course);

    Course update(Course course);

    Optional<Course> findByIdAndUserId(long id, long userId);

    List<Course> findAllByUserId(long userId);
}
