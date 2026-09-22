package com.niniyumi.personalagent.course.application;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseAudioPart;
import com.niniyumi.personalagent.course.domain.CourseAudioPartRepository;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.infrastructure.media.CourseAudioSegmenter;
import com.niniyumi.personalagent.course.infrastructure.media.CourseMediaException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CourseImportProcessor {
    private static final Logger log = LoggerFactory.getLogger(CourseImportProcessor.class);
    private final CourseRepository courses;
    private final CourseAudioPartRepository parts;
    private final CourseAudioSegmenter segmenter;
    private final CourseProcessingService processing;
    private final Clock clock;

    public CourseImportProcessor(CourseRepository courses, CourseAudioPartRepository parts,
            CourseAudioSegmenter segmenter, CourseProcessingService processing, Clock clock) {
        this.courses = courses;
        this.parts = parts;
        this.segmenter = segmenter;
        this.processing = processing;
        this.clock = clock;
    }

    @Async("courseTaskExecutor")
    public void processAsync(long userId, long courseId) { process(userId, courseId); }

    public void process(long userId, long courseId) {
        Course course = courses.findByIdAndUserId(courseId, userId).orElseThrow(CourseNotFoundException::new);
        String stage = "校验导入状态";
        try {
            if (course.status() != CourseStatus.PROCESSING || !"IMPORT".equals(course.sourceType())
                    || course.originalAudioPath() == null) throw new InvalidCourseStateException();
            List<CourseAudioPart> existing = parts.findAllByCourseId(courseId);
            if (existing.isEmpty()) {
                Path original = Path.of(course.originalAudioPath());
                stage = "切分录音";
                log.info("开始切分课程录音, userId={}, courseId={}, fileBytes={}, storagePath={}",
                        userId, courseId, course.expectedBytes(), course.originalAudioPath());
                List<CourseAudioSegmenter.Segment> segments = segmenter.split(original, original.getParent());
                stage = "保存录音分段";
                // 输入文件已由 ffprobe 限制为 9000 秒；各段向上取整会产生少量累计误差。
                int totalSeconds = Math.min(9000,
                        segments.stream().mapToInt(CourseAudioSegmenter.Segment::durationSeconds).sum());
                for (int index = 0; index < segments.size(); index++) {
                    var segment = segments.get(index);
                    parts.save(new CourseAudioPart(null, courseId, index + 1, segment.durationSeconds(),
                            segment.path().toString(), segment.fileSize(), clock.instant()));
                }
                course = courses.update(new Course(course.id(), course.userId(), course.title(),
                        CourseStatus.PROCESSING, totalSeconds, 10, null, null, null,
                        course.sourceType(), course.originalAudioPath(), course.expectedBytes(),
                        course.createdAt(), clock.instant()).withLesson(course.courseName(), course.lessonDate()));
                long totalBytes = segments.stream().mapToLong(CourseAudioSegmenter.Segment::fileSize).sum();
                log.info("课程录音切分完成, userId={}, courseId={}, parts={}, durationSeconds={}, fileBytes={}, storagePath={}",
                        userId, courseId, segments.size(), totalSeconds, totalBytes, course.originalAudioPath());
            }
            stage = "转写录音";
            processing.process(userId, courseId);
        } catch (RuntimeException exception) {
            log.error("课程录音处理失败, userId={}, courseId={}, stage={}, storagePath={}",
                    userId, courseId, stage, course.originalAudioPath(), exception);
            String message = exception instanceof CourseMediaException ? exception.getMessage() : "录音处理失败，请重试";
            courses.update(new Course(course.id(), course.userId(), course.title(), CourseStatus.FAILED,
                    course.durationSeconds(), course.processingProgress(), course.transcript(), null, message,
                    course.sourceType(), course.originalAudioPath(), course.expectedBytes(),
                    course.createdAt(), clock.instant()).withLesson(course.courseName(), course.lessonDate()));
        }
    }
}
