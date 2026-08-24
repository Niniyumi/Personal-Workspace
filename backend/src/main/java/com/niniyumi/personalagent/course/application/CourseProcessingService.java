package com.niniyumi.personalagent.course.application;

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
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CourseProcessingService {
    private static final String NOTE_PROMPT = """
            你是课程笔记助手。请根据完整课堂转写生成简洁的 Markdown 笔记，固定包含：
            课程摘要、核心知识点、重要概念、示例或案例、复习提纲。不要编造转写中不存在的内容。
            """;
    private final CourseRepository courses;
    private final CourseAudioPartRepository parts;
    private final CourseAudioStorage storage;
    private final SpeechProvider speechProvider;
    private final ChatProvider chatProvider;
    private final Clock clock;

    public CourseProcessingService(
            CourseRepository courses,
            CourseAudioPartRepository parts,
            CourseAudioStorage storage,
            SpeechProvider speechProvider,
            ChatProvider chatProvider,
            Clock clock) {
        this.courses = courses;
        this.parts = parts;
        this.storage = storage;
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
        try {
            String transcript = audioParts.stream()
                    .map(part -> speechProvider.transcribe(Path.of(part.storagePath())))
                    .reduce((left, right) -> left + "\n\n" + right)
                    .orElseThrow(InvalidCoursePartsException::new);
            String note = chatProvider.complete(NOTE_PROMPT, transcript);
            courses.update(new Course(
                    course.id(), course.userId(), course.title(), CourseStatus.READY,
                    course.durationSeconds(), transcript, note.trim(), null,
                    course.createdAt(), clock.instant()));

        } catch (RuntimeException exception) {
            courses.update(new Course(
                    course.id(), course.userId(), course.title(), CourseStatus.FAILED,
                    course.durationSeconds(), null, null, "转写或笔记生成失败，请重新处理",
                    course.createdAt(), clock.instant()));
            return;
        }

        // 清理失败不影响已生成的笔记，残留临时文件之后可人工删除。
        try {
            List<Path> paths = audioParts.stream().map(part -> Path.of(part.storagePath())).toList();
            storage.deleteAll(paths);
            parts.deleteAllByCourseId(courseId);
        } catch (RuntimeException ignored) {
            // READY 是最终业务结果，不能因临时文件清理失败被降级为 FAILED。
        }
    }
}
