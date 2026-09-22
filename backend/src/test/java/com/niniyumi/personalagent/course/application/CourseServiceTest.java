package com.niniyumi.personalagent.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
                Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC),
                new TranscriptSanitizer());
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
    void keepsEachLessonIdAndCourseGroupingAfterRenaming() {
        Course first = service.create(42L, "管理学", LocalDate.of(2026, 9, 1));
        Course second = service.create(42L, "管理学", LocalDate.of(2026, 9, 8));

        service.rename(42L, first.id(), "管理学第一讲");

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(service.get(42L, first.id()).title()).isEqualTo("管理学第一讲");
        assertThat(service.get(42L, first.id()).courseName()).isEqualTo("管理学");
        assertThat(service.get(42L, first.id()).lessonDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(service.get(42L, second.id()).lessonDate()).isEqualTo(LocalDate.of(2026, 9, 8));
    }

    @Test
    void renamesAnOwnedCourseWithoutChangingItsContent() {
        Course created = service.create(42L, "原课程名");
        repository.update(new Course(
                created.id(), created.userId(), created.title(), CourseStatus.READY, 80,
                100, "完整转写", "课程笔记", null, created.createdAt(), created.updatedAt()));

        String renamed = service.rename(42L, created.id(), "  新笔记名称  ");
        Course stored = repository.findByIdAndUserId(created.id(), 42L).orElseThrow();

        assertThat(renamed).isEqualTo("新笔记名称");
        assertThat(stored.title()).isEqualTo("新笔记名称");
        assertThat(stored.transcript()).isEqualTo("完整转写");
        assertThat(stored.noteContent()).isEqualTo("课程笔记");
    }

    @Test
    void rejectsABlankRenamedTitle() {
        Course created = service.create(42L, "原课程名");

        assertThatThrownBy(() -> service.rename(42L, created.id(), "  "))
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

    @Test
    void startsRegenerationWithoutDeletingTheSavedNote() {
        Course created = service.create(42L, "Spring Boot");
        repository.update(new Course(
                created.id(), created.userId(), created.title(), CourseStatus.READY, 80,
                100, "完整转写", "旧笔记", null, created.createdAt(), created.updatedAt()));

        Course processing = service.beginNoteGeneration(42L, created.id());

        assertThat(processing.status()).isEqualTo(CourseStatus.PROCESSING);
        assertThat(processing.noteContent()).isEqualTo("旧笔记");
    }

    @Test
    void resolvesARegeneratedCandidateByAppendingOrReplacingIt() {
        Course created = service.create(42L, "Spring Boot");
        repository.update(new Course(
                created.id(), created.userId(), created.title(), CourseStatus.READY, 80,
                100, "完整转写", "旧笔记", null, "RECORDING", null, null,
                "新生成笔记", created.createdAt(), created.updatedAt()));

        Course appended = service.resolveNoteCandidate(42L, created.id(), NoteCandidateAction.APPEND);
        assertThat(appended.noteContent()).contains("旧笔记").contains("新生成笔记");
        assertThat(appended.noteCandidate()).isNull();

        repository.update(new Course(
                appended.id(), appended.userId(), appended.title(), CourseStatus.READY, 80,
                100, "完整转写", "旧笔记", null, "RECORDING", null, null,
                "替换笔记", appended.createdAt(), appended.updatedAt()));
        assertThat(service.resolveNoteCandidate(42L, created.id(), NoteCandidateAction.REPLACE).noteContent())
                .isEqualTo("替换笔记");
    }

    private static final class InMemoryCourseRepository implements CourseRepository {
        private final List<Course> courses = new ArrayList<>();

        @Override
        public Course save(Course course) {
            Course saved = new Course(
                    (long) courses.size() + 1,
                    course.userId(), course.title(), course.status(), course.durationSeconds(),
                    course.processingProgress(), course.transcript(), course.noteContent(), course.errorMessage(),
                    course.createdAt(), course.updatedAt()).withLesson(course.courseName(), course.lessonDate());
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
