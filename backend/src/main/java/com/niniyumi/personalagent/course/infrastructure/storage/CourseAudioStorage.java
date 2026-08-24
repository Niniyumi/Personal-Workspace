package com.niniyumi.personalagent.course.infrastructure.storage;

import java.nio.file.Path;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CourseAudioStorage {
    Path store(long userId, long courseId, int partNumber, MultipartFile file);

    void deleteAll(List<Path> paths);
}
