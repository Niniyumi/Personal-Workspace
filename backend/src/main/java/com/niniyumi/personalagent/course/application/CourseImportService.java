package com.niniyumi.personalagent.course.application;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.infrastructure.storage.CourseImportStorage;
import com.niniyumi.personalagent.course.infrastructure.media.CourseAudioInspector;
import com.niniyumi.personalagent.course.infrastructure.media.CourseAudioFormat;
import com.niniyumi.personalagent.course.infrastructure.media.CourseMediaException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CourseImportService {
    private static final Logger log = LoggerFactory.getLogger(CourseImportService.class);
    public static final long MAX_FILE_BYTES = 512L * 1024 * 1024;
    private final CourseRepository courses;
    private final CourseImportStorage storage;
    private final CourseAudioInspector inspector;
    private final Clock clock;

    public CourseImportService(CourseRepository courses, CourseImportStorage storage,
            CourseAudioInspector inspector, Clock clock) {
        this.courses = courses;
        this.storage = storage;
        this.inspector = inspector;
        this.clock = clock;
    }

    public Course create(long userId, String title, long expectedBytes, String fileName) {
        return create(userId, title, expectedBytes, fileName, null);
    }

    public Course create(long userId, String title, long expectedBytes, String fileName,
            java.time.LocalDate lessonDate) {
        if (title == null || title.isBlank()) throw new InvalidCourseTitleException();
        if (expectedBytes < 1 || expectedBytes > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Audio file size is not supported");
        }
        CourseAudioFormat format = CourseAudioFormat.fromFileName(fileName);
        Course course = courses.save(new Course(null, userId, title.trim(), CourseStatus.UPLOADING,
                0, 0, null, null, null, "IMPORT", null, expectedBytes, clock.instant(), clock.instant())
                .withLesson(title.trim(), lessonDate == null
                        ? java.time.LocalDate.now(clock.withZone(java.time.ZoneId.of("Asia/Shanghai"))) : lessonDate));
        String originalPath = storage.original(userId, course.id(), format.extension()).toString();
        course = courses.update(new Course(course.id(), course.userId(), course.title(), course.status(),
                course.durationSeconds(), course.processingProgress(), course.transcript(), course.noteContent(),
                course.errorMessage(), course.sourceType(), originalPath, course.expectedBytes(),
                course.createdAt(), clock.instant()).withLesson(course.courseName(), course.lessonDate()));
        log.info("开始接收课程录音, userId={}, courseId={}, fileName={}, fileBytes={}, format={}",
                userId, course.id(), fileName, expectedBytes, format.extension());
        return course;
    }

    public long offset(long userId, long courseId) {
        uploading(userId, courseId);
        return storage.offset(userId, courseId);
    }

    public long append(long userId, long courseId, long offset, InputStream content) {
        Course course = uploading(userId, courseId);
        long saved = storage.append(userId, courseId, offset, content, course.expectedBytes());
        log.info("课程录音接收进度, userId={}, courseId={}, receivedBytes={}, totalBytes={}",
                userId, courseId, saved, course.expectedBytes());
        return saved;
    }

    public Course finish(long userId, long courseId) {
        Course course = uploading(userId, courseId);
        if (storage.offset(userId, courseId) != course.expectedBytes()) {
            throw new IllegalArgumentException("Audio upload is incomplete");
        }
        CourseAudioFormat expectedFormat = CourseAudioFormat.fromFileName(course.originalAudioPath());
        CourseAudioFormat detectedFormat = inspector.inspect(storage.uploadPath(userId, courseId));
        if (detectedFormat != expectedFormat) {
            throw new CourseMediaException("录音文件内容与扩展名不一致");
        }
        String path = storage.finish(userId, courseId, course.expectedBytes(),
                Path.of(course.originalAudioPath())).toString();
        Course updated = courses.update(new Course(course.id(), course.userId(), course.title(),
                CourseStatus.PROCESSING, 0, 0, null, null, null,
                course.sourceType(), path, course.expectedBytes(), course.createdAt(), clock.instant())
                .withLesson(course.courseName(), course.lessonDate()));
        log.info("课程录音接收完成, userId={}, courseId={}, fileBytes={}, storagePath={}",
                userId, courseId, course.expectedBytes(), path);
        return updated;
    }

    public Course retry(long userId, long courseId) {
        Course course = courses.findByIdAndUserId(courseId, userId).orElseThrow(CourseNotFoundException::new);
        if (course.status() != CourseStatus.FAILED || !"IMPORT".equals(course.sourceType())
                || course.originalAudioPath() == null) throw new InvalidCourseStateException();
        return courses.update(new Course(course.id(), course.userId(), course.title(),
                CourseStatus.PROCESSING, course.durationSeconds(), course.processingProgress(),
                course.transcript(), null, null, course.sourceType(), course.originalAudioPath(),
                course.expectedBytes(), course.createdAt(), clock.instant())
                .withLesson(course.courseName(), course.lessonDate()));
    }

    public Resource original(long userId, long courseId) {
        Course course = courses.findByIdAndUserId(courseId, userId).orElseThrow(CourseNotFoundException::new);
        if (!"IMPORT".equals(course.sourceType()) || course.originalAudioPath() == null) {
            throw new CourseNotFoundException();
        }
        FileSystemResource resource = new FileSystemResource(Path.of(course.originalAudioPath()));
        if (!resource.exists()) throw new CourseNotFoundException();
        return resource;
    }

    private Course uploading(long userId, long courseId) {
        Course course = courses.findByIdAndUserId(courseId, userId).orElseThrow(CourseNotFoundException::new);
        if (course.status() != CourseStatus.UPLOADING || !"IMPORT".equals(course.sourceType())) {
            throw new InvalidCourseStateException();
        }
        return course;
    }
}
