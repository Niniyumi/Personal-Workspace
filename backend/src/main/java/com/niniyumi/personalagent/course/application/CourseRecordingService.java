package com.niniyumi.personalagent.course.application;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseAudioPart;
import com.niniyumi.personalagent.course.domain.CourseAudioPartRepository;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.infrastructure.storage.CourseAudioStorage;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CourseRecordingService {
    public static final int MAX_DURATION_SECONDS = 5400;
    public static final int MAX_PARTS = 18;
    public static final long MAX_TOTAL_BYTES = 180L * 1024 * 1024;
    private static final Set<String> AUDIO_TYPES = Set.of(
            "audio/webm", "audio/ogg", "audio/mp4", "audio/mpeg", "audio/wav", "audio/x-wav");
    private final CourseService courseService;
    private final CourseRepository courses;
    private final CourseAudioPartRepository parts;
    private final CourseAudioStorage storage;
    private final Clock clock;

    public CourseRecordingService(
            CourseService courseService,
            CourseRepository courses,
            CourseAudioPartRepository parts,
            CourseAudioStorage storage,
            Clock clock) {
        this.courseService = courseService;
        this.courses = courses;
        this.parts = parts;
        this.storage = storage;
        this.clock = clock;
    }

    public CourseAudioPart uploadPart(
            long userId, long courseId, int partNumber, int durationSeconds, MultipartFile file) {
        Course course = courseService.get(userId, courseId);
        if (course.status() != CourseStatus.RECORDING) {
            throw new InvalidCourseStateException();
        }
        if (partNumber < 1 || partNumber > MAX_PARTS
                || durationSeconds < 1 || durationSeconds > 360
                || file.isEmpty() || !AUDIO_TYPES.contains(file.getContentType())) {
            throw new InvalidCoursePartsException();
        }
        // 网络重试可能重复提交同一分片，直接返回已有记录即可。
        CourseAudioPart existing = parts.findByCourseIdAndPartNumber(courseId, partNumber).orElse(null);
        if (existing != null) return existing;

        List<CourseAudioPart> uploaded = parts.findAllByCourseId(courseId);
        int totalDuration = uploaded.stream().mapToInt(CourseAudioPart::durationSeconds).sum();
        long totalBytes = uploaded.stream().mapToLong(CourseAudioPart::fileSize).sum();
        if (totalDuration + durationSeconds > MAX_DURATION_SECONDS
                || totalBytes + file.getSize() > MAX_TOTAL_BYTES) {
            throw new InvalidCoursePartsException();
        }
        Path path = storage.store(userId, courseId, partNumber, file);
        return parts.save(new CourseAudioPart(
                null, courseId, partNumber, durationSeconds, path.toString(), file.getSize(), clock.instant()));
    }

    public Course complete(long userId, long courseId) {
        Course course = courseService.get(userId, courseId);
        if (course.status() != CourseStatus.RECORDING) {
            throw new InvalidCourseStateException();
        }
        List<CourseAudioPart> uploaded = parts.findAllByCourseId(courseId);
        int total = 0;
        for (int index = 0; index < uploaded.size(); index++) {
            CourseAudioPart part = uploaded.get(index);
            if (part.partNumber() != index + 1) {
                throw new InvalidCoursePartsException();
            }
            total += part.durationSeconds();
        }
        if (uploaded.isEmpty() || total > MAX_DURATION_SECONDS) {
            throw new InvalidCoursePartsException();
        }
        return courses.update(new Course(
                course.id(), course.userId(), course.title(), CourseStatus.PROCESSING, total,
                10, null, null, null, course.createdAt(), clock.instant()));
    }

    public Course retry(long userId, long courseId) {
        Course course = courseService.get(userId, courseId);
        if (course.status() != CourseStatus.FAILED || parts.findAllByCourseId(courseId).isEmpty()) {
            throw new InvalidCourseStateException();
        }
        return courses.update(new Course(
                course.id(), course.userId(), course.title(), CourseStatus.PROCESSING,
                course.durationSeconds(), 10, null, null, null, course.createdAt(), clock.instant()));
    }
}
