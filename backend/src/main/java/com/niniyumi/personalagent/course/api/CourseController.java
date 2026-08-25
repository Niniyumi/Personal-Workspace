package com.niniyumi.personalagent.course.api;

import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import com.niniyumi.personalagent.course.api.dto.CourseAudioPartResponse;
import com.niniyumi.personalagent.course.api.dto.CourseResponse;
import com.niniyumi.personalagent.course.api.dto.CourseSummaryResponse;
import com.niniyumi.personalagent.course.api.dto.CreateCourseRequest;
import com.niniyumi.personalagent.course.api.dto.UpdateCourseNoteRequest;
import com.niniyumi.personalagent.course.application.CourseProcessingService;
import com.niniyumi.personalagent.course.application.CourseRecordingService;
import com.niniyumi.personalagent.course.application.CourseService;
import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.application.InvalidCourseStateException;
import com.niniyumi.personalagent.course.infrastructure.document.CourseDocxExporter;
import jakarta.validation.Valid;
import java.util.List;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;
    private final CourseRecordingService recordingService;
    private final CourseProcessingService processingService;
    private final CourseDocxExporter docxExporter;

    public CourseController(
            CourseService courseService,
            CourseRecordingService recordingService,
            CourseProcessingService processingService,
            CourseDocxExporter docxExporter) {
        this.courseService = courseService;
        this.recordingService = recordingService;
        this.processingService = processingService;
        this.docxExporter = docxExporter;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CourseResponse.from(courseService.create(user.userId(), request.title())));
    }

    @GetMapping
    public List<CourseSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return courseService.list(user.userId()).stream().map(CourseSummaryResponse::from).toList();
    }

    @GetMapping("/{courseId}")
    public CourseResponse get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId) {
        return CourseResponse.from(courseService.get(user.userId(), courseId));
    }

    @PostMapping(value = "/{courseId}/parts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CourseAudioPartResponse uploadPart(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId,
            @RequestParam int partNumber,
            @RequestParam int durationSeconds,
            @RequestPart("file") MultipartFile file) {
        return CourseAudioPartResponse.from(recordingService.uploadPart(
                user.userId(), courseId, partNumber, durationSeconds, file));
    }

    @PostMapping("/{courseId}/complete")
    public ResponseEntity<CourseResponse> complete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId) {
        Course course = recordingService.complete(user.userId(), courseId);
        // HTTP 请求立即返回，耗时转写在线程池中执行。
        processingService.processAsync(user.userId(), courseId);
        return ResponseEntity.accepted().body(CourseResponse.from(course));
    }

    @PostMapping("/{courseId}/retry")
    public ResponseEntity<CourseResponse> retry(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId) {
        Course course = recordingService.retry(user.userId(), courseId);
        processingService.processAsync(user.userId(), courseId);
        return ResponseEntity.accepted().body(CourseResponse.from(course));
    }

    @PutMapping("/{courseId}/note")
    public CourseResponse saveNote(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId,
            @Valid @RequestBody UpdateCourseNoteRequest request) {
        return CourseResponse.from(courseService.saveNote(user.userId(), courseId, request.noteContent()));
    }

    @GetMapping(value = "/{courseId}/note.docx",
            produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<byte[]> downloadNote(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId) {
        Course course = courseService.get(user.userId(), courseId);
        if (course.status() != CourseStatus.READY) throw new InvalidCourseStateException();
        String disposition = ContentDisposition.attachment()
                .filename(course.title() + ".docx", StandardCharsets.UTF_8)
                .build().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(docxExporter.export(course));
    }
}
