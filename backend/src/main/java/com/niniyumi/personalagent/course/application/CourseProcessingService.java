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
            你是忠实的课程笔记整理助手。课堂转写是唯一事实来源。
            只总结老师在转写中明确讲过的内容，不得添加模型自己的知识、观点、推断、结论或案例。
            不得生成课后自测、练习题、延伸阅读、学习建议或转写中没有的复习内容。
            老师没有明确布置任务时，不得生成课后任务。无法从转写确认的内容直接省略，不得猜测。
            使用简洁 Markdown，可按实际内容使用：课程摘要、老师讲解的知识点、课堂中明确出现的例子、老师明确布置的任务。
            没有对应内容的栏目直接省略，不要为了凑齐栏目自行补写，并保持老师原有结论和因果关系。
            """;
    private final CourseRepository courses;
    private final CourseAudioPartRepository parts;
    private final SpeechProvider speechProvider;
    private final ChatProvider chatProvider;
    private final Clock clock;
    private final TranscriptSanitizer transcriptSanitizer;

    public CourseProcessingService(
            CourseRepository courses,
            CourseAudioPartRepository parts,
            SpeechProvider speechProvider,
            ChatProvider chatProvider,
            Clock clock,
            TranscriptSanitizer transcriptSanitizer) {
        this.courses = courses;
        this.parts = parts;
        this.speechProvider = speechProvider;
        this.chatProvider = chatProvider;
        this.clock = clock;
        this.transcriptSanitizer = transcriptSanitizer;
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
            long totalBytes = audioParts.stream().mapToLong(CourseAudioPart::fileSize).sum();
            log.info("开始课程录音转写, userId={}, courseId={}, parts={}, durationSeconds={}, fileBytes={}",
                    userId, courseId, audioParts.size(), course.durationSeconds(), totalBytes);
            courses.updateProgress(withProgress(course, progress));
            if (audioParts.isEmpty()) throw new InvalidCoursePartsException();
            List<String> transcripts = new ArrayList<>();
            for (int index = 0; index < audioParts.size(); index++) {
                CourseAudioPart part = audioParts.get(index);
                String text = part.transcript();
                if (text == null || text.isBlank()) {
                    log.info("开始转写课程录音分段, userId={}, courseId={}, partNumber={}, durationSeconds={}, fileBytes={}, storagePath={}",
                            userId, courseId, part.partNumber(), part.durationSeconds(),
                            part.fileSize(), part.storagePath());
                    text = transcriptSanitizer.clean(speechProvider.transcribe(Path.of(part.storagePath())));
                    parts.update(new CourseAudioPart(part.id(), part.courseId(), part.partNumber(),
                            part.durationSeconds(), part.storagePath(), part.fileSize(), text, part.createdAt()));
                    log.info("课程录音分段转写成功, userId={}, courseId={}, partNumber={}, transcriptChars={}",
                            userId, courseId, part.partNumber(), text.length());
                }
                transcripts.add(text);
                progress = 10 + Math.round(65F * (index + 1) / audioParts.size());
                courses.updateProgress(withProgress(course, progress));
            }
            String transcript = String.join("\n\n", transcripts);
            courses.update(new Course(
                    course.id(), course.userId(), course.title(), CourseStatus.TRANSCRIBED,
                    course.durationSeconds(), 100, transcript, null, null,
                    course.sourceType(), course.originalAudioPath(), course.expectedBytes(),
                    null, course.createdAt(), clock.instant()).withLesson(course.courseName(), course.lessonDate()));
            log.info("课程录音转写完成, userId={}, courseId={}, parts={}, durationSeconds={}, fileBytes={}, transcriptChars={}, storagePath={}",
                    userId, courseId, audioParts.size(), course.durationSeconds(), totalBytes,
                    transcript.length(), course.originalAudioPath());

        } catch (RuntimeException exception) {
            log.error("课程录音转写失败, userId={}, courseId={}, completedParts={}, storagePath={}",
                    userId, courseId, completedParts(audioParts), course.originalAudioPath(), exception);
            courses.update(new Course(
                    course.id(), course.userId(), course.title(), CourseStatus.FAILED,
                    course.durationSeconds(), progress, partialTranscript(audioParts), null, processingErrorMessage(exception),
                    course.sourceType(), course.originalAudioPath(), course.expectedBytes(),
                    null, course.createdAt(), clock.instant()).withLesson(course.courseName(), course.lessonDate()));
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
            String transcript = transcriptSanitizer.clean(course.transcript());
            log.info("开始生成课程笔记, userId={}, courseId={}, transcriptChars={}, regeneration={}",
                    userId, courseId, transcript.length(), course.noteContent() != null);
            String note = chatProvider.complete(NOTE_PROMPT, transcript).trim();
            boolean regeneration = course.noteContent() != null && !course.noteContent().isBlank();
            courses.update(new Course(
                    course.id(), course.userId(), course.title(), CourseStatus.READY,
                    course.durationSeconds(), 100, transcript,
                    regeneration ? course.noteContent() : note, null,
                    course.sourceType(), course.originalAudioPath(), course.expectedBytes(),
                    regeneration ? note : null, course.createdAt(), clock.instant())
                    .withLesson(course.courseName(), course.lessonDate()));
            log.info("课程笔记生成完成, userId={}, courseId={}, transcriptChars={}, noteChars={}",
                    userId, courseId, transcript.length(), note.length());
        } catch (RuntimeException exception) {
            log.error("课程笔记生成失败, userId={}, courseId={}, transcriptChars={}",
                    userId, courseId, course.transcript().length(), exception);
            boolean regeneration = course.noteContent() != null && !course.noteContent().isBlank();
            courses.update(new Course(
                    course.id(), course.userId(), course.title(),
                    regeneration ? CourseStatus.READY : CourseStatus.TRANSCRIBED,
                    course.durationSeconds(), 100, transcriptSanitizer.clean(course.transcript()), course.noteContent(),
                    noteErrorMessage(exception), course.sourceType(), course.originalAudioPath(),
                    course.expectedBytes(), null, course.createdAt(), clock.instant())
                    .withLesson(course.courseName(), course.lessonDate()));
        }
    }

    private Course withProgress(Course course, int progress) {
        return withProgress(course, progress, course.transcript());
    }

    private Course withProgress(Course course, int progress, String transcript) {
        return new Course(course.id(), course.userId(), course.title(), CourseStatus.PROCESSING,
                course.durationSeconds(), progress, transcript, course.noteContent(), null,
                course.sourceType(), course.originalAudioPath(), course.expectedBytes(),
                course.noteCandidate(), course.createdAt(), clock.instant())
                .withLesson(course.courseName(), course.lessonDate());
    }

    private String partialTranscript(List<CourseAudioPart> audioParts) {
        if (audioParts.isEmpty()) return null;
        String text = parts.findAllByCourseId(audioParts.get(0).courseId()).stream()
                .map(CourseAudioPart::transcript).filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining("\n\n"));
        return text.isBlank() ? null : text;
    }

    private long completedParts(List<CourseAudioPart> audioParts) {
        return parts.findAllByCourseId(audioParts.isEmpty() ? -1 : audioParts.get(0).courseId()).stream()
                .filter(part -> part.transcript() != null && !part.transcript().isBlank()).count();
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
