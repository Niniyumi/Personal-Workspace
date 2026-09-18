package com.niniyumi.personalagent.course.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseAudioPart;
import com.niniyumi.personalagent.course.domain.CourseAudioPartRepository;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.infrastructure.media.CourseAudioSegmenter;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class CourseImportProcessorTest {
    private final Instant now = Instant.parse("2026-09-13T12:00:00Z");
    private Course value = new Course(9L, 42L, "网络课", CourseStatus.PROCESSING, 0, 0,
            null, null, null, "IMPORT", "audio/original.m4a", 10L, now, now);
    private final List<CourseAudioPart> savedParts = new ArrayList<>();
    private final CourseRepository courses = new CourseRepository() {
        public Course save(Course course) { value = course; return course; }
        public Course update(Course course) { value = course; return course; }
        public Optional<Course> findByIdAndUserId(long id, long userId) {
            return id == 9 && userId == 42 ? Optional.of(value) : Optional.empty();
        }
        public List<Course> findAllByUserId(long userId) { return List.of(value); }
    };
    private final CourseAudioPartRepository parts = new CourseAudioPartRepository() {
        public CourseAudioPart save(CourseAudioPart part) { savedParts.add(part); return part; }
        public CourseAudioPart update(CourseAudioPart part) {
            savedParts.set(part.partNumber() - 1, part);
            return part;
        }
        public Optional<CourseAudioPart> findByCourseIdAndPartNumber(long id, int number) { return Optional.empty(); }
        public List<CourseAudioPart> findAllByCourseId(long id) { return List.copyOf(savedParts); }
        public void deleteAllByCourseId(long id) { savedParts.clear(); }
    };

    @Test
    void splitsTheOriginalThenTranscribesEachSegment(CapturedOutput output) {
        CourseAudioSegmenter segmenter = (original, outputDirectory) -> List.of(
                new CourseAudioSegmenter.Segment(Path.of("part-001.webm"), 270, 1024),
                new CourseAudioSegmenter.Segment(Path.of("part-002.webm"), 30, 128));
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        CourseProcessingService processing = new CourseProcessingService(courses, parts,
                path -> path.toString().contains("001") ? "第一段" : "第二段",
                (system, user) -> "not called", clock, new TranscriptSanitizer());

        new CourseImportProcessor(courses, parts, segmenter, processing, clock).process(42, 9);

        assertThat(value.status()).isEqualTo(CourseStatus.TRANSCRIBED);
        assertThat(value.durationSeconds()).isEqualTo(300);
        assertThat(value.transcript()).isEqualTo("第一段\n\n第二段");
        assertThat(savedParts).hasSize(2);
        assertThat(value.originalAudioPath()).isEqualTo("audio/original.m4a");
        assertThat(output).contains(
                "开始切分课程录音, userId=42, courseId=9, fileBytes=10, storagePath=audio/original.m4a",
                "课程录音切分完成, userId=42, courseId=9, parts=2, durationSeconds=300, fileBytes=1152");
    }

    @Test
    void logsTheStageWhenSplittingFails(CapturedOutput output) {
        CourseAudioSegmenter segmenter = (original, outputDirectory) -> {
            throw new IllegalStateException("ffmpeg failed");
        };
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        CourseProcessingService processing = new CourseProcessingService(courses, parts,
                path -> "not called", (system, user) -> "not called", clock, new TranscriptSanitizer());

        new CourseImportProcessor(courses, parts, segmenter, processing, clock).process(42, 9);

        assertThat(value.status()).isEqualTo(CourseStatus.FAILED);
        assertThat(output).contains(
                "课程录音处理失败, userId=42, courseId=9, stage=切分录音, storagePath=audio/original.m4a");
    }

    @Test
    void allowsAValidatedRecordingAtTheDurationLimitDespiteSegmentRounding() {
        CourseAudioSegmenter segmenter = (original, output) -> IntStream.rangeClosed(1, 34)
                .mapToObj(number -> new CourseAudioSegmenter.Segment(
                        Path.of("part-" + number + ".webm"), 265, 1024)).toList();
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        CourseProcessingService processing = new CourseProcessingService(courses, parts,
                path -> "文字", (system, user) -> "not called", clock, new TranscriptSanitizer());

        new CourseImportProcessor(courses, parts, segmenter, processing, clock).process(42, 9);

        assertThat(value.status()).isEqualTo(CourseStatus.TRANSCRIBED);
        assertThat(value.durationSeconds()).isEqualTo(9000);
    }
}
