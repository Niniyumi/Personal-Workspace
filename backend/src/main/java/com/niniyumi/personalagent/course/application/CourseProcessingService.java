package com.niniyumi.personalagent.course.application;

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
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CourseProcessingService {
    private static final Logger log = LoggerFactory.getLogger(CourseProcessingService.class);
    private static final String NOTE_PROMPT = """
            你是课程笔记助手。请根据完整课堂转写生成简洁的 Markdown 笔记，固定包含：
            课程摘要、核心知识点、重要概念、示例或案例、复习提纲。不要编造转写中不存在的内容。
            """;
    private final CourseRepository courses;
    private final CourseAudioPartRepository parts;
    private final SpeechProvider speechProvider;
    private final ChatProvider chatProvider;
    private final Clock clock;

    public CourseProcessingService(
            CourseRepository courses,
            CourseAudioPartRepository parts,
            SpeechProvider speechProvider,
            ChatProvider chatProvider,
            Clock clock) {
        this.courses = courses;
        this.parts = parts;
        this.speechProvider = speechProvider;
        this.chatProvider = chatProvider;
        this.clock = clock;
    }

    @Async("courseTaskExecutor")
    public void processAsync(long userId, long courseId) {
        process(userId, courseId);
    }

    public void process(long userId, long courseId) {
        Course course = courses.findByIdAndUserId(courseId, userId)
                .orElseThrow(CourseNotFoundException::new);
        List<CourseAudioPart> audioParts = parts.findAllByCourseId(courseId);
        int progress = 10;
        try {
            courses.update(withProgress(course, progress));
            if (audioParts.isEmpty()) throw new InvalidCoursePartsException();
            List<String> transcripts = new ArrayList<>();
            for (int index = 0; index < audioParts.size(); index++) {
                transcripts.add(speechProvider.transcribe(Path.of(audioParts.get(index).storagePath())));
                progress = 10 + Math.round(65F * (index + 1) / audioParts.size());
                courses.update(withProgress(course, progress));
            }
            String transcript = String.join("\n\n", transcripts);
            courses.update(new Course(
                    course.id(), course.userId(), course.title(), CourseStatus.TRANSCRIBED,
                    course.durationSeconds(), 100, transcript, null, null,
                    course.createdAt(), clock.instant()));

        } catch (RuntimeException exception) {
            log.error("Course processing failed, userId={}, courseId={}", userId, courseId, exception);
            courses.update(new Course(
                    course.id(), course.userId(), course.title(), CourseStatus.FAILED,
                    course.durationSeconds(), progress, null, null, processingErrorMessage(exception),
                    course.createdAt(), clock.instant()));
            return;
        }

    }

    @Async("courseTaskExecutor")
    public void generateNoteAsync(long userId, long courseId) {
        generateNote(userId, courseId);
    }

    public void generateNote(long userId, long courseId) {
        Course course = courses.findByIdAndUserId(courseId, userId)
                .orElseThrow(CourseNotFoundException::new);
        if (course.status() != CourseStatus.PROCESSING
                || course.transcript() == null || course.transcript().isBlank()) {
            throw new InvalidCourseStateException();
        }
        try {
            String note = chatProvider.complete(NOTE_PROMPT, course.transcript());
            courses.update(new Course(
                    course.id(), course.userId(), course.title(), CourseStatus.READY,
                    course.durationSeconds(), 100, course.transcript(), note.trim(), null,
                    course.createdAt(), clock.instant()));
        } catch (RuntimeException exception) {
            log.error("Course note generation failed, userId={}, courseId={}", userId, courseId, exception);
            courses.update(new Course(
                    course.id(), course.userId(), course.title(), CourseStatus.TRANSCRIBED,
                    course.durationSeconds(), 100, course.transcript(), null,
                    noteErrorMessage(exception), course.createdAt(), clock.instant()));
        }
    }

    private Course withProgress(Course course, int progress) {
        return new Course(course.id(), course.userId(), course.title(), CourseStatus.PROCESSING,
                course.durationSeconds(), progress, course.transcript(), course.noteContent(), null,
                course.createdAt(), clock.instant());
    }

    private String processingErrorMessage(RuntimeException exception) {
        if (exception instanceof SpeechProviderException
                && "Speech provider is not configured".equals(exception.getMessage())) {
            return "语音识别服务未配置，请设置 DASHSCOPE_API_KEY";
        }
        return "录音转写失败，请重新处理";
    }

    private String noteErrorMessage(RuntimeException exception) {
        if (exception instanceof AiProviderException
                && "AI provider is not configured".equals(exception.getMessage())) {
            return "笔记模型未配置，请设置 DASHSCOPE_API_KEY";
        }
        return "笔记生成失败，请重试";
    }
}
