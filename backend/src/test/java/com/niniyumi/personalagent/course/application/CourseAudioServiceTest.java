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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CourseAudioServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");
    private final Course course = new Course(9L, 42L, "Java", CourseStatus.TRANSCRIBED,
            20, 100, "转写", null, null, NOW, NOW);
    private final CourseRepository courses = new SingleCourseRepository(course);
    private final CourseAudioPart part = new CourseAudioPart(
            3L, 9L, 1, 20, "audio/9/1.webm", 5, NOW);
    private final CourseAudioPartRepository parts = new SinglePartRepository(part);
    private final CourseAudioStorage storage = new CourseAudioStorage() {
        public Path store(long userId, long courseId, int partNumber,
                org.springframework.web.multipart.MultipartFile file) { throw new UnsupportedOperationException(); }
        public byte[] read(Path path) { return "audio".getBytes(); }
        public void deleteAll(List<Path> paths) { }
    };

    @Test
    void listsAndReadsAudioAfterCheckingCourseOwnership() {
        CourseAudioService service = service();

        assertThat(service.list(42L, 9L)).containsExactly(part);
        assertThat(service.read(42L, 9L, 1)).isEqualTo("audio".getBytes());
    }

    @Test
    void doesNotExposeAnotherUsersAudio() {
        CourseAudioService service = service();

        assertThatThrownBy(() -> service.read(7L, 9L, 1))
                .isInstanceOf(CourseNotFoundException.class);
    }

    private CourseAudioService service() {
        return new CourseAudioService(
                new CourseService(courses, Clock.fixed(NOW, ZoneOffset.UTC)), parts, storage);
    }

    private record SingleCourseRepository(Course course) implements CourseRepository {
        public Course save(Course value) { return value; }
        public Course update(Course value) { return value; }
        public Optional<Course> findByIdAndUserId(long id, long userId) {
            return course.id() == id && course.userId() == userId ? Optional.of(course) : Optional.empty();
        }
        public List<Course> findAllByUserId(long userId) { return List.of(course); }
    }

    private record SinglePartRepository(CourseAudioPart part) implements CourseAudioPartRepository {
        public CourseAudioPart save(CourseAudioPart value) { return value; }
        public Optional<CourseAudioPart> findByCourseIdAndPartNumber(long courseId, int partNumber) {
            return part.courseId() == courseId && part.partNumber() == partNumber ? Optional.of(part) : Optional.empty();
        }
        public List<CourseAudioPart> findAllByCourseId(long courseId) { return List.of(part); }
        public void deleteAllByCourseId(long courseId) { }
    }
}
