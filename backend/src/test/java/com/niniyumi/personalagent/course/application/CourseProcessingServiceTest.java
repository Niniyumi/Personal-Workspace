package com.niniyumi.personalagent.course.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseAudioPart;
import com.niniyumi.personalagent.course.domain.CourseAudioPartRepository;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.infrastructure.speech.SpeechProvider;
import com.niniyumi.personalagent.course.infrastructure.speech.SpeechProviderException;
import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.ChatProvider;
import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.AiProviderException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class CourseProcessingServiceTest {
    private final MemoryCourseRepository courses = new MemoryCourseRepository();
    private final MemoryPartRepository parts = new MemoryPartRepository();
    private final Instant now = Instant.parse("2026-08-24T12:00:00Z");

    @BeforeEach
    void setUp() {
        courses.value = new Course(9L, 42L, "计算机网络", CourseStatus.PROCESSING, 600,
                0, null, null, null, now, now);
        parts.values.add(new CourseAudioPart(1L, 9L, 1, 300, "audio/9/1.webm", 10, now));
        parts.values.add(new CourseAudioPart(2L, 9L, 2, 300, "audio/9/2.webm", 10, now));
    }

    @Test
    void savesTranscriptWithoutGeneratingNotesOrDeletingAudio(CapturedOutput output) {
        SpeechProvider speech = path -> path.toString().endsWith("1.webm") ? "第一部分" : "第二部分";
        List<String> noteRequests = new ArrayList<>();
        ChatProvider chat = (system, user) -> {
            noteRequests.add(user);
            return "# 课程摘要\n整理完成";
        };
        CourseProcessingService service = service(speech, chat);

        service.process(42L, 9L);

        assertThat(courses.value.status().name()).isEqualTo("TRANSCRIBED");
        assertThat(courses.value.transcript()).isEqualTo("第一部分\n\n第二部分");
        assertThat(courses.value.noteContent()).isNull();
        assertThat(courses.history).extracting(Course::processingProgress)
                .containsExactly(10, 43, 75, 100);
        assertThat(noteRequests).isEmpty();
        assertThat(parts.values).hasSize(2);
        assertThat(output).contains(
                "课程录音转写完成, userId=42, courseId=9, parts=2, durationSeconds=600, fileBytes=20, transcriptChars=10");
    }

    @Test
    void keepsAudioAndMarksTheCourseFailedWhenTranscriptionFails(CapturedOutput output) {
        CourseProcessingService service = service(path -> { throw new RuntimeException("provider down"); },
                (system, user) -> "不会执行");

        service.process(42L, 9L);

        assertThat(courses.value.status()).isEqualTo(CourseStatus.FAILED);
        assertThat(courses.value.errorMessage()).isEqualTo("录音转写失败，请重新处理");
        assertThat(parts.values).hasSize(2);
        assertThat(output).contains("课程录音转写失败, userId=42, courseId=9");
    }

    @Test
    void explainsWhenTheSpeechProviderIsNotConfigured() {
        CourseProcessingService service = service(
                path -> { throw new SpeechProviderException("Speech provider is not configured"); },
                (system, user) -> "不会执行");

        service.process(42L, 9L);

        assertThat(courses.value.status()).isEqualTo(CourseStatus.FAILED);
        assertThat(courses.value.errorMessage()).isEqualTo("语音识别服务未配置，请设置 DASHSCOPE_API_KEY");
    }

    @Test
    void generatesANoteFromTheSavedTranscript() {
        List<String> systemPrompts = new ArrayList<>();
        courses.value = new Course(9L, 42L, "计算机网络", CourseStatus.PROCESSING, 600,
                85, "完整课堂转写", null, null, now, now);
        CourseProcessingService service = service(path -> "不会执行", (system, user) -> {
            systemPrompts.add(system);
            return "# 笔记";
        });

        service.generateNote(42L, 9L);

        assertThat(courses.value.status()).isEqualTo(CourseStatus.READY);
        assertThat(courses.value.transcript()).isEqualTo("完整课堂转写");
        assertThat(courses.value.noteContent()).isEqualTo("# 笔记");
        assertThat(systemPrompts).singleElement().asString()
                .contains("课堂转写是唯一事实来源")
                .contains("不得生成课后自测、练习题、延伸阅读")
                .contains("老师没有明确布置任务时，不得生成课后任务");
    }

    @Test
    void resumesAfterTheFirstSuccessfulSegmentWithoutRetranscribingIt() {
        List<String> calls = new ArrayList<>();
        CourseProcessingService first = service(path -> {
            calls.add(path.toString());
            if (path.toString().endsWith("2.webm")) throw new RuntimeException("temporary failure");
            return "已保存第一段";
        }, (system, user) -> "not called");
        first.process(42L, 9L);
        assertThat(courses.value.status()).isEqualTo(CourseStatus.FAILED);
        assertThat(parts.values.get(0).transcript()).isEqualTo("已保存第一段");
        courses.value = new Course(9L, 42L, "计算机网络", CourseStatus.PROCESSING, 600,
                10, courses.value.transcript(), null, null, now, now);

        service(path -> {
            calls.add(path.toString());
            return "第二段";
        }, (system, user) -> "not called").process(42L, 9L);

        assertThat(calls).hasSize(3);
        assertThat(courses.value.transcript()).isEqualTo("已保存第一段\n\n第二段");
    }

    @Test
    void keepsTheTranscriptAvailableWhenNoteGenerationFails() {
        courses.value = new Course(9L, 42L, "计算机网络", CourseStatus.PROCESSING, 600,
                85, "完整课堂转写", null, null, now, now);
        CourseProcessingService service = service(path -> "不会执行", (system, user) -> {
            throw new AiProviderException("provider unavailable");
        });

        service.generateNote(42L, 9L);

        assertThat(courses.value.status()).isEqualTo(CourseStatus.TRANSCRIBED);
        assertThat(courses.value.transcript()).isEqualTo("完整课堂转写");
        assertThat(courses.value.noteContent()).isNull();
        assertThat(courses.value.errorMessage()).isEqualTo("笔记生成失败，请重试");
    }

    private CourseProcessingService service(SpeechProvider speech, ChatProvider chat) {
        return new CourseProcessingService(courses, parts, speech, chat,
                Clock.fixed(now.plusSeconds(60), ZoneOffset.UTC));
    }

    private static final class MemoryCourseRepository implements CourseRepository {
        private Course value;
        private final List<Course> history = new ArrayList<>();
        public Course save(Course course) { value = course; return course; }
        public Course update(Course course) { value = course; history.add(course); return course; }
        public Optional<Course> findByIdAndUserId(long id, long userId) {
            return value != null && value.id() == id && value.userId() == userId ? Optional.of(value) : Optional.empty();
        }
        public List<Course> findAllByUserId(long userId) { return value == null ? List.of() : List.of(value); }
    }

    private static final class MemoryPartRepository implements CourseAudioPartRepository {
        private final List<CourseAudioPart> values = new ArrayList<>();
        public CourseAudioPart save(CourseAudioPart part) { values.add(part); return part; }
        public CourseAudioPart update(CourseAudioPart part) {
            values.set(part.partNumber() - 1, part);
            return part;
        }
        public Optional<CourseAudioPart> findByCourseIdAndPartNumber(long courseId, int partNumber) {
            return values.stream().filter(value -> value.courseId() == courseId && value.partNumber() == partNumber).findFirst();
        }
        public List<CourseAudioPart> findAllByCourseId(long courseId) { return List.copyOf(values); }
        public void deleteAllByCourseId(long courseId) { values.clear(); }
    }

}
