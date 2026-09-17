package com.niniyumi.personalagent.course.infrastructure.media;

import java.nio.file.Path;
import java.util.List;

public interface CourseAudioSegmenter {
    List<Segment> split(Path original, Path outputDirectory);

    record Segment(Path path, int durationSeconds, long fileSize) { }
}
