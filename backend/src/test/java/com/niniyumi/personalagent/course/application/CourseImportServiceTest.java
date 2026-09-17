package com.niniyumi.personalagent.course.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.infrastructure.config.CourseProperties;
import com.niniyumi.personalagent.course.infrastructure.storage.CourseImportStorage;
import com.niniyumi.personalagent.course.infrastructure.media.CourseMediaException;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class CourseImportServiceTest {
    @TempDir Path root;
    private Course value;
    private final CourseRepository courses = new CourseRepository() {
        public Course save(Course course) { value = courseWithId(course); return value; }
        public Course update(Course course) { value = course; return course; }
        public Optional<Course> findByIdAndUserId(long id, long userId) {
            return value != null && value.id() == id && value.userId() == userId ? Optional.of(value) : Optional.empty();
        }
        public List<Course> findAllByUserId(long userId) { return List.of(); }
    };

    @Test
    void storesOriginalBeforeStartingTranscription(CapturedOutput output) throws Exception {
        CourseImportService service = service();
        Course created = service.create(42, "网络课", 6, "lecture.mp3");
        assertThat(created.status()).isEqualTo(CourseStatus.UPLOADING);
        assertThat(created.sourceType()).isEqualTo("IMPORT");
        assertThat(created.originalAudioPath()).endsWith("original.mp3");
        assertThat(service.append(42, created.id(), 0, new ByteArrayInputStream("abcdef".getBytes())))
                .isEqualTo(6);

        Course completed = service.finish(42, created.id());
        assertThat(completed.status()).isEqualTo(CourseStatus.PROCESSING);
        assertThat(Files.readString(Path.of(completed.originalAudioPath()))).isEqualTo("abcdef");
        assertThat(output)
                .contains("开始接收课程录音, userId=42, courseId=9, fileName=lecture.mp3, fileBytes=6")
                .contains("课程录音接收完成, userId=42, courseId=9, fileBytes=6, storagePath=");
    }

    @Test
    void rejectsAnotherUsersUploadBeforeWritingAnyData() {
        CourseImportService service = service();
        Course created = service.create(42, "网络课", 6, "lecture.wav");
        assertThatThrownBy(() -> service.append(7, created.id(), 0,
                new ByteArrayInputStream("abcdef".getBytes())))
                .isInstanceOf(CourseNotFoundException.class);
        assertThat(service.offset(42, created.id())).isZero();
    }

    @Test
    void rejectsFilesOverTheSupportedSize() {
        assertThatThrownBy(() -> service().create(42, "网络课", 512L * 1024 * 1024 + 1, "lecture.m4a"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsOnlyM4aMp3AndWavExtensions() {
        CourseImportService service = service();

        assertThatThrownBy(() -> service.create(42, "网络课", 3, "lecture.aac"))
                .isInstanceOf(CourseMediaException.class);
    }

    @Test
    void retryPreservesTheOriginalAfterConversionFailed() throws Exception {
        CourseImportService service = service();
        service.create(42, "网络课", 3, "lecture.m4a");
        service.append(42, 9, 0, new ByteArrayInputStream("abc".getBytes()));
        Course completed = service.finish(42, 9);
        value = new Course(9L, 42L, "网络课", CourseStatus.FAILED, 0, 0,
                null, null, "FFmpeg failed", "IMPORT", completed.originalAudioPath(), 3L,
                completed.createdAt(), completed.updatedAt());

        Course retried = service.retry(42, 9);

        assertThat(retried.status()).isEqualTo(CourseStatus.PROCESSING);
        assertThat(retried.originalAudioPath()).isEqualTo(completed.originalAudioPath());
    }

    @Test
    void letsOnlyTheOwnerReadTheSavedOriginal() throws Exception {
        CourseImportService service = service();
        service.create(42, "网络课", 3, "lecture.wav");
        service.append(42, 9, 0, new ByteArrayInputStream("abc".getBytes()));
        service.finish(42, 9);

        assertThat(service.original(42, 9).getContentAsByteArray()).isEqualTo("abc".getBytes());
        assertThatThrownBy(() -> service.original(7, 9)).isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void rejectsInvalidM4aBeforeMarkingTheUploadComplete() {
        CourseImportService service = new CourseImportService(courses,
                new CourseImportStorage(new CourseProperties(root.toString())),
                path -> { throw new CourseMediaException("无效录音"); },
                Clock.fixed(Instant.parse("2026-09-13T12:00:00Z"), ZoneOffset.UTC));
        service.create(42, "网络课", 3, "lecture.m4a");
        service.append(42, 9, 0, new ByteArrayInputStream("bad".getBytes()));

        assertThatThrownBy(() -> service.finish(42, 9)).isInstanceOf(CourseMediaException.class);
        assertThat(value.status()).isEqualTo(CourseStatus.UPLOADING);
        assertThat(service.offset(42, 9)).isEqualTo(3);
    }

    @Test
    void rejectsARecordingWhoseContentDoesNotMatchItsExtension() {
        CourseImportService service = new CourseImportService(courses,
                new CourseImportStorage(new CourseProperties(root.toString())),
                path -> com.niniyumi.personalagent.course.infrastructure.media.CourseAudioFormat.WAV,
                Clock.fixed(Instant.parse("2026-09-13T12:00:00Z"), ZoneOffset.UTC));
        service.create(42, "网络课", 3, "lecture.mp3");
        service.append(42, 9, 0, new ByteArrayInputStream("bad".getBytes()));

        assertThatThrownBy(() -> service.finish(42, 9)).isInstanceOf(CourseMediaException.class);
        assertThat(value.status()).isEqualTo(CourseStatus.UPLOADING);
    }

    private CourseImportService service() {
        return new CourseImportService(courses, new CourseImportStorage(new CourseProperties(root.toString())),
                path -> com.niniyumi.personalagent.course.infrastructure.media.CourseAudioFormat
                        .fromFileName(value.originalAudioPath()),
                Clock.fixed(Instant.parse("2026-09-13T12:00:00Z"), ZoneOffset.UTC));
    }

    private Course courseWithId(Course course) {
        return new Course(9L, course.userId(), course.title(), course.status(), course.durationSeconds(),
                course.processingProgress(), course.transcript(), course.noteContent(), course.errorMessage(),
                course.sourceType(), course.originalAudioPath(), course.expectedBytes(),
                course.createdAt(), course.updatedAt());
    }
}
