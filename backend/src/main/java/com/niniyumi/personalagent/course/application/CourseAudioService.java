package com.niniyumi.personalagent.course.application;

import com.niniyumi.personalagent.course.domain.CourseAudioPart;
import com.niniyumi.personalagent.course.domain.CourseAudioPartRepository;
import com.niniyumi.personalagent.course.infrastructure.storage.CourseAudioStorage;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CourseAudioService {
    private final CourseService courseService;
    private final CourseAudioPartRepository parts;
    private final CourseAudioStorage storage;

    public CourseAudioService(
            CourseService courseService,
            CourseAudioPartRepository parts,
            CourseAudioStorage storage) {
        this.courseService = courseService;
        this.parts = parts;
        this.storage = storage;
    }

    public List<CourseAudioPart> list(long userId, long courseId) {
        courseService.get(userId, courseId);
        return parts.findAllByCourseId(courseId);
    }

    public byte[] read(long userId, long courseId, int partNumber) {
        courseService.get(userId, courseId);
        CourseAudioPart part = parts.findByCourseIdAndPartNumber(courseId, partNumber)
                .orElseThrow(CourseNotFoundException::new);
        return storage.read(Path.of(part.storagePath()));
    }
}
