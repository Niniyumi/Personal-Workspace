package com.niniyumi.personalagent.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CourseServiceTest {
    private final InMemoryCourseRepository repository = new InMemoryCourseRepository();
    private CourseService service;

    @BeforeEach
    void setUp() {
        service = new CourseService(
                repository,
                Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsARecordingCourseForTheCurrentUser() {
        Course course = service.create(42L, "  Java 并发课  ");

        assertThat(course.id()).isEqualTo(1L);
        assertThat(course.userId()).isEqualTo(42L);
        assertThat(course.title()).isEqualTo("Java 并发课");
        assertThat(course.status()).isEqualTo(CourseStatus.RECORDING);
        assertThat(course.durationSeconds()).isZero();
    }

    @Test
    void rejectsABlankCourseTitle() {
        assertThatThrownBy(() -> service.create(42L, "  "))
                .isInstanceOf(InvalidCourseTitleException.class);
    }

    @Test
    void doesNotExposeAnotherUsersCourse() {
        Course course = service.create(42L, "数据库系统");

        assertThatThrownBy(() -> service.get(7L, course.id()))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void savesAnEditedNoteForTheOwner() {
        Course created = service.create(42L, "Spring Boot");
        repository.update(new Course(
                created.id(), created.userId(), created.title(), CourseStatus.READY, 80,
                100, "完整转写", "旧笔记", null, created.createdAt(), created.updatedAt()));

        Course updated = service.saveNote(42L, created.id(), "  # 新笔记  ");

        assertThat(updated.noteContent()).isEqualTo("# 新笔记");
        assertThat(updated.updatedAt()).isEqualTo(Instant.parse("2026-08-24T12:00:00Z"));
    }

    @Test
    void onlyAllowsEditingACompletedCourseNote() {
        Course recording = service.create(42L, "Spring Boot");

        assertThatThrownBy(() -> service.saveNote(42L, recording.id(), "新笔记"))
                .isInstanceOf(InvalidCourseStateException.class);
    }

    @Test
    void beginsNoteGenerationOnlyAfterTranscription() {
        Course created = service.create(42L, "Spring Boot");
        repository.update(new Course(
                created.id(), created.userId(), created.title(), CourseStatus.TRANSCRIBED, 80,
                100, "完整转写", null, null, created.createdAt(), created.updatedAt()));

        Course processing = service.beginNoteGeneration(42L, created.id());

        assertThat(processing.status()).isEqualTo(CourseStatus.PROCESSING);
        assertThat(processing.processingProgress()).isEqualTo(85);
        assertThat(processing.transcript()).isEqualTo("完整转写");
    }

    @Test
    void rejectsNoteGenerationBeforeTranscription() {
        Course created = service.create(42L, "Spring Boot");

        assertThatThrownBy(() -> service.beginNoteGeneration(42L, created.id()))
                .isInstanceOf(InvalidCourseStateException.class);
    }

    private static final class InMemoryCourseRepository implements CourseRepository {
        private final List<Course> courses = new ArrayList<>();

        @Override
        public Course save(Course course) {
            Course saved = new Course(
                    (long) courses.size() + 1,
                    course.userId(), course.title(), course.status(), course.durationSeconds(),
                    course.processingProgress(), course.transcript(), course.noteContent(), course.errorMessage(),
                    course.createdAt(), course.updatedAt());
            courses.add(saved);
            return saved;
        }

        @Override
        public Course update(Course course) {
            courses.removeIf(existing -> existing.id().equals(course.id()));
            courses.add(course);
            return course;
        }

        @Override
        public Optional<Course> findByIdAndUserId(long id, long userId) {
            return courses.stream()
                    .filter(course -> course.id() == id && course.userId() == userId)
                    .findFirst();
        }

        @Override
        public List<Course> findAllByUserId(long userId) {
            return courses.stream().filter(course -> course.userId() == userId).toList();
        }
    }
}
