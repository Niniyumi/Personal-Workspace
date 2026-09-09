package com.niniyumi.personalagent.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseAudioPart;
import com.niniyumi.personalagent.course.domain.CourseAudioPartRepository;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.infrastructure.storage.CourseAudioStorage;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CourseRecordingServiceTest {
    private final MemoryCourseRepository courses = new MemoryCourseRepository();
    private final MemoryPartRepository parts = new MemoryPartRepository();
    private final MemoryStorage storage = new MemoryStorage();
    private CourseRecordingService service;

    @BeforeEach
    void setUp() {
        CourseService courseService = new CourseService(
                courses, Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC));
        Course course = courseService.create(42L, "操作系统");
        service = new CourseRecordingService(courseService, courses, parts, storage,
                Clock.fixed(Instant.parse("2026-08-24T12:01:00Z"), ZoneOffset.UTC));
        assertThat(course.id()).isEqualTo(1L);
    }

    @Test
    void uploadingTheSamePartTwiceReturnsTheExistingPart() {
        MockMultipartFile file = new MockMultipartFile("file", "part.webm", "audio/webm", "audio".getBytes());

        CourseAudioPart first = service.uploadPart(42L, 1L, 1, 300, file);
        CourseAudioPart second = service.uploadPart(42L, 1L, 1, 300, file);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(parts.findAllByCourseId(1L)).hasSize(1);
        assertThat(storage.storedPaths).hasSize(1);
    }

    @Test
    void completesOnlyWhenPartNumbersAreContinuous() {
        service.uploadPart(42L, 1L, 2, 20,
                new MockMultipartFile("file", "part.webm", "audio/webm", "audio".getBytes()));

        assertThatThrownBy(() -> service.complete(42L, 1L))
                .isInstanceOf(InvalidCoursePartsException.class);
    }

    @Test
    void completeMovesTheCourseToProcessingWithTotalDuration() {
        service.uploadPart(42L, 1L, 1, 300,
                new MockMultipartFile("file", "one.webm", "audio/webm", "one".getBytes()));
        service.uploadPart(42L, 1L, 2, 20,
                new MockMultipartFile("file", "two.webm", "audio/webm", "two".getBytes()));

        Course course = service.complete(42L, 1L);

        assertThat(course.status()).isEqualTo(CourseStatus.PROCESSING);
        assertThat(course.durationSeconds()).isEqualTo(320);
    }

    @Test
    void rejectsPartsThatWouldExceedTheNinetyMinuteLimit() {
        for (int partNumber = 1; partNumber <= 15; partNumber++) {
            service.uploadPart(42L, 1L, partNumber, 360,
                    new MockMultipartFile("file", "part.webm", "audio/webm", "audio".getBytes()));
        }

        assertThatThrownBy(() -> service.uploadPart(42L, 1L, 16, 1,
                new MockMultipartFile("file", "part.webm", "audio/webm", "audio".getBytes())))
                .isInstanceOf(InvalidCoursePartsException.class);
    }

    @Test
    void rejectsUnexpectedFileTypesAndPartNumbers() {
        assertThatThrownBy(() -> service.uploadPart(42L, 1L, 19, 10,
                new MockMultipartFile("file", "part.webm", "audio/webm", "audio".getBytes())))
                .isInstanceOf(InvalidCoursePartsException.class);
        assertThatThrownBy(() -> service.uploadPart(42L, 1L, 1, 10,
                new MockMultipartFile("file", "part.txt", "text/plain", "audio".getBytes())))
                .isInstanceOf(InvalidCoursePartsException.class);
    }

    private static final class MemoryStorage implements CourseAudioStorage {
        private final List<Path> storedPaths = new ArrayList<>();

        @Override
        public Path store(long userId, long courseId, int partNumber, org.springframework.web.multipart.MultipartFile file) {
            Path path = Path.of("audio", Long.toString(courseId), partNumber + ".webm");
            storedPaths.add(path);
            return path;
        }

        @Override
        public byte[] read(Path path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteAll(List<Path> paths) {
            storedPaths.removeAll(paths);
        }
    }

    private static final class MemoryCourseRepository implements CourseRepository {
        private final List<Course> values = new ArrayList<>();

        public Course save(Course course) {
            Course saved = new Course((long) values.size() + 1, course.userId(), course.title(), course.status(),
                    course.durationSeconds(), course.processingProgress(), course.transcript(), course.noteContent(), course.errorMessage(),
                    course.createdAt(), course.updatedAt());
            values.add(saved);
            return saved;
        }

        public Course update(Course course) {
            values.removeIf(value -> value.id().equals(course.id()));
            values.add(course);
            return course;
        }

        public Optional<Course> findByIdAndUserId(long id, long userId) {
            return values.stream().filter(value -> value.id() == id && value.userId() == userId).findFirst();
        }

        public List<Course> findAllByUserId(long userId) {
            return values.stream().filter(value -> value.userId() == userId).toList();
        }
    }

    private static final class MemoryPartRepository implements CourseAudioPartRepository {
        private final List<CourseAudioPart> values = new ArrayList<>();

        public CourseAudioPart save(CourseAudioPart part) {
            CourseAudioPart saved = new CourseAudioPart((long) values.size() + 1, part.courseId(), part.partNumber(),
                    part.durationSeconds(), part.storagePath(), part.fileSize(), part.createdAt());
            values.add(saved);
            return saved;
        }

        public Optional<CourseAudioPart> findByCourseIdAndPartNumber(long courseId, int partNumber) {
            return values.stream().filter(value -> value.courseId() == courseId && value.partNumber() == partNumber).findFirst();
        }

        public List<CourseAudioPart> findAllByCourseId(long courseId) {
            return values.stream().filter(value -> value.courseId() == courseId)
                    .sorted(Comparator.comparingInt(CourseAudioPart::partNumber)).toList();
        }

        public void deleteAllByCourseId(long courseId) {
            values.removeIf(value -> value.courseId() == courseId);
        }
    }
}
