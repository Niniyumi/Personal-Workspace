package com.niniyumi.personalagent.course.infrastructure.media;

import java.nio.file.Path;

@FunctionalInterface
public interface CourseAudioInspector {
    CourseAudioFormat inspect(Path original);
}
