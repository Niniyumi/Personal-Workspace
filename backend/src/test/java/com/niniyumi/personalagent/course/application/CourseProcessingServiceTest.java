package com.niniyumi.personalagent.course.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseAudioPart;
import com.niniyumi.personalagent.course.domain.CourseAudioPartRepository;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.infrastructure.speech.SpeechProvider;
import com.niniyumi.personalagent.course.infrastructure.storage.CourseAudioStorage;
import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.ChatProvider;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CourseProcessingServiceTest {
    private final MemoryCourseRepository courses = new MemoryCourseRepository();
    private final MemoryPartRepository parts = new MemoryPartRepository();
    private final MemoryStorage storage = new MemoryStorage();
    private final Instant now = Instant.parse("2026-08-24T12:00:00Z");

    @BeforeEach
    void setUp() {
        courses.value = new Course(9L, 42L, "计算机网络", CourseStatus.PROCESSING, 600,
                null, null, null, now, now);
        parts.values.add(new CourseAudioPart(1L, 9L, 1, 300, "audio/9/1.webm", 10, now));
        parts.values.add(new CourseAudioPart(2L, 9L, 2, 300, "audio/9/2.webm", 10, now));
    }

    @Test
    void joinsTranscriptsGeneratesNotesAndDeletesSuccessfulAudio() {
        SpeechProvider speech = path -> path.toString().endsWith("1.webm") ? "第一部分" : "第二部分";
        ChatProvider chat = (system, user) -> "# 课程摘要\n整理完成";
        CourseProcessingService service = service(speech, chat);

        service.process(42L, 9L);

        assertThat(courses.value.status()).isEqualTo(CourseStatus.READY);
        assertThat(courses.value.transcript()).isEqualTo("第一部分\n\n第二部分");
        assertThat(courses.value.noteContent()).startsWith("# 课程摘要");
        assertThat(parts.values).isEmpty();
        assertThat(storage.deleted).containsExactly(Path.of("audio/9/1.webm"), Path.of("audio/9/2.webm"));
    }

    @Test
    void keepsAudioAndMarksTheCourseFailedWhenTranscriptionFails() {
        CourseProcessingService service = service(path -> { throw new RuntimeException("provider down"); },
                (system, user) -> "不会执行");

        service.process(42L, 9L);

        assertThat(courses.value.status()).isEqualTo(CourseStatus.FAILED);
        assertThat(courses.value.errorMessage()).isEqualTo("转写或笔记生成失败，请重新处理");
        assertThat(parts.values).hasSize(2);
        assertThat(storage.deleted).isEmpty();
    }

    @Test
    void keepsAReadyResultWhenTemporaryAudioCleanupFails() {
        storage.failDeletion = true;
        CourseProcessingService service = service(path -> "课堂内容", (system, user) -> "# 笔记");

        service.process(42L, 9L);

        assertThat(courses.value.status()).isEqualTo(CourseStatus.READY);
        assertThat(courses.value.transcript()).isEqualTo("课堂内容\n\n课堂内容");
    }

    private CourseProcessingService service(SpeechProvider speech, ChatProvider chat) {
        return new CourseProcessingService(courses, parts, storage, speech, chat,
                Clock.fixed(now.plusSeconds(60), ZoneOffset.UTC));
    }

    private static final class MemoryCourseRepository implements CourseRepository {
        private Course value;
        public Course save(Course course) { value = course; return course; }
        public Course update(Course course) { value = course; return course; }
        public Optional<Course> findByIdAndUserId(long id, long userId) {
            return value != null && value.id() == id && value.userId() == userId ? Optional.of(value) : Optional.empty();
        }
        public List<Course> findAllByUserId(long userId) { return value == null ? List.of() : List.of(value); }
    }

    private static final class MemoryPartRepository implements CourseAudioPartRepository {
        private final List<CourseAudioPart> values = new ArrayList<>();
        public CourseAudioPart save(CourseAudioPart part) { values.add(part); return part; }
        public Optional<CourseAudioPart> findByCourseIdAndPartNumber(long courseId, int partNumber) {
            return values.stream().filter(value -> value.courseId() == courseId && value.partNumber() == partNumber).findFirst();
        }
        public List<CourseAudioPart> findAllByCourseId(long courseId) { return List.copyOf(values); }
        public void deleteAllByCourseId(long courseId) { values.clear(); }
    }

    private static final class MemoryStorage implements CourseAudioStorage {
        private final List<Path> deleted = new ArrayList<>();
        private boolean failDeletion;
        public Path store(long userId, long courseId, int partNumber, org.springframework.web.multipart.MultipartFile file) {
            throw new UnsupportedOperationException();
        }
        public void deleteAll(List<Path> paths) {
            if (failDeletion) throw new RuntimeException("disk busy");
            deleted.addAll(paths);
        }
    }
}
