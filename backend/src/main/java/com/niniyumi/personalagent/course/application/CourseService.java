package com.niniyumi.personalagent.course.application;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CourseService {
    private static final Logger log = LoggerFactory.getLogger(CourseService.class);
    private final CourseRepository repository;
    private final Clock clock;
    private final TranscriptSanitizer transcriptSanitizer;

    public CourseService(CourseRepository repository, Clock clock, TranscriptSanitizer transcriptSanitizer) {
        this.repository = repository;
        this.clock = clock;
        this.transcriptSanitizer = transcriptSanitizer;
    }

    public Course create(long userId, String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidCourseTitleException();
        }
        Instant now = clock.instant();
        return repository.save(new Course(
                null, userId, title.trim(), CourseStatus.RECORDING, 0,
                0, null, null, null, now, now));
    }

    public Course get(long userId, long courseId) {
        // 查询条件始终带 userId，避免仅凭课程编号读取其他用户的数据。
        return repository.findByIdAndUserId(courseId, userId)
                .map(this::withCleanTranscript)
                .orElseThrow(CourseNotFoundException::new);
    }

    public List<Course> list(long userId) {
        return repository.findAllByUserId(userId).stream().map(this::withCleanTranscript).toList();
    }

    public Course saveNote(long userId, long courseId, String noteContent) {
        Course current = get(userId, courseId);
        if (current.status() != CourseStatus.READY) {
            throw new InvalidCourseStateException();
        }
        Course updated = new Course(
                current.id(), current.userId(), current.title(), current.status(),
                current.durationSeconds(), current.processingProgress(), current.transcript(), normalize(noteContent),
                current.errorMessage(), current.sourceType(), current.originalAudioPath(), current.expectedBytes(),
                current.noteCandidate(), current.createdAt(), clock.instant());
        return repository.update(updated);
    }

    public Course beginNoteGeneration(long userId, long courseId) {
        Course current = get(userId, courseId);
        if ((current.status() != CourseStatus.TRANSCRIBED && current.status() != CourseStatus.READY)
                || current.transcript() == null || current.transcript().isBlank()) {
            throw new InvalidCourseStateException();
        }
        return repository.update(new Course(
                current.id(), current.userId(), current.title(), CourseStatus.PROCESSING,
                current.durationSeconds(), 85, current.transcript(), current.noteContent(), null,
                current.sourceType(), current.originalAudioPath(), current.expectedBytes(),
                null, current.createdAt(), clock.instant()));
    }

    public Course resolveNoteCandidate(long userId, long courseId, NoteCandidateAction action) {
        Course current = get(userId, courseId);
        if (current.status() != CourseStatus.READY
                || current.noteCandidate() == null || current.noteCandidate().isBlank()) {
            throw new InvalidCourseStateException();
        }
        String note = switch (action) {
            case REPLACE -> current.noteCandidate();
            case APPEND -> (current.noteContent() == null || current.noteContent().isBlank())
                    ? current.noteCandidate()
                    : current.noteContent() + "\n\n---\n\n## 补充笔记\n\n" + current.noteCandidate();
            case DISCARD -> current.noteContent();
        };
        log.info("处理重新生成的课程笔记, userId={}, courseId={}, action={}, oldChars={}, candidateChars={}",
                userId, courseId, action,
                current.noteContent() == null ? 0 : current.noteContent().length(),
                current.noteCandidate().length());
        return repository.update(new Course(
                current.id(), current.userId(), current.title(), CourseStatus.READY,
                current.durationSeconds(), 100, current.transcript(), note, null,
                current.sourceType(), current.originalAudioPath(), current.expectedBytes(),
                null, current.createdAt(), clock.instant()));
    }

    private Course withCleanTranscript(Course course) {
        String cleaned = transcriptSanitizer.clean(course.transcript());
        if (java.util.Objects.equals(cleaned, course.transcript())) return course;
        return new Course(course.id(), course.userId(), course.title(), course.status(),
                course.durationSeconds(), course.processingProgress(), cleaned, course.noteContent(),
                course.errorMessage(), course.sourceType(), course.originalAudioPath(), course.expectedBytes(),
                course.noteCandidate(), course.createdAt(), course.updatedAt());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
