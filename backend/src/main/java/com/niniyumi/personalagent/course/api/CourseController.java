package com.niniyumi.personalagent.course.api;

import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import com.niniyumi.personalagent.course.api.dto.CourseAudioPartResponse;
import com.niniyumi.personalagent.course.api.dto.CourseResponse;
import com.niniyumi.personalagent.course.api.dto.CourseSummaryResponse;
import com.niniyumi.personalagent.course.api.dto.CourseTitleResponse;
import com.niniyumi.personalagent.course.api.dto.CreateCourseRequest;
import com.niniyumi.personalagent.course.api.dto.CreateCourseImportRequest;
import com.niniyumi.personalagent.course.api.dto.UpdateCourseNoteRequest;
import com.niniyumi.personalagent.course.api.dto.UpdateCourseTitleRequest;
import com.niniyumi.personalagent.course.api.dto.ResolveNoteCandidateRequest;
import com.niniyumi.personalagent.course.application.CourseProcessingService;
import com.niniyumi.personalagent.course.application.CourseAudioService;
import com.niniyumi.personalagent.course.application.CourseRecordingService;
import com.niniyumi.personalagent.course.application.CourseService;
import com.niniyumi.personalagent.course.application.CourseImportService;
import com.niniyumi.personalagent.course.application.CourseImportProcessor;
import com.niniyumi.personalagent.course.application.CoursePlaybackService;
import com.niniyumi.personalagent.course.domain.CourseProgress;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.application.InvalidCourseStateException;
import com.niniyumi.personalagent.course.infrastructure.document.CourseDocxExporter;
import com.niniyumi.personalagent.course.infrastructure.media.CourseAudioFormat;
import jakarta.validation.Valid;
import java.util.List;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
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
    private final CourseAudioService courseAudioService;
    private final CourseImportService importService;
    private final CourseImportProcessor importProcessor;
    private final CoursePlaybackService playbackService;

    public CourseController(
            CourseService courseService,
            CourseRecordingService recordingService,
            CourseProcessingService processingService,
            CourseDocxExporter docxExporter,
            CourseAudioService courseAudioService,
            CourseImportService importService,
            CourseImportProcessor importProcessor,
            CoursePlaybackService playbackService) {
        this.courseService = courseService;
        this.recordingService = recordingService;
        this.processingService = processingService;
        this.docxExporter = docxExporter;
        this.courseAudioService = courseAudioService;
        this.importService = importService;
        this.importProcessor = importProcessor;
        this.playbackService = playbackService;
    }

    /** 创建一个支持分块上传的本地录音导入任务。 */
    @PostMapping("/imports")
    public ResponseEntity<CourseResponse> createImport(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateCourseImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CourseResponse.from(importService.create(
                        user.userId(), request.title(), request.fileSize(), request.fileName(), request.lessonDate())));
    }

    /** 查询录音导入任务已经保存的字节偏移，用于断点续传。 */
    @GetMapping("/imports/{courseId}")
    public UploadOffset importOffset(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long courseId) {
        return new UploadOffset(importService.offset(user.userId(), courseId));
    }

    /** 从指定偏移追加一块录音文件数据。 */
    @PutMapping(value = "/imports/{courseId}/chunk", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public UploadOffset uploadImportChunk(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId, @RequestParam long offset, HttpServletRequest request) {
        try {
            return new UploadOffset(importService.append(user.userId(), courseId, offset, request.getInputStream()));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** 确认录音文件上传完成，并异步开始切段和转写。 */
    @PostMapping("/imports/{courseId}/complete")
    public ResponseEntity<CourseResponse> completeImport(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long courseId) {
        Course course = importService.finish(user.userId(), courseId);
        importProcessor.processAsync(user.userId(), courseId);
        return ResponseEntity.accepted().body(CourseResponse.from(course));
    }

    public record UploadOffset(long offset) { }

    /** 为课程原始录音签发一个短期播放地址。 */
    @PostMapping("/{courseId}/original/playback")
    public PlaybackUrl originalPlayback(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long courseId) {
        String ticket = playbackService.issue(user.userId(), courseId);
        return new PlaybackUrl("/api/course-audio/" + ticket);
    }

    public record PlaybackUrl(String url) { }

    /** 下载当前用户拥有的课程原始录音。 */
    @GetMapping("/{courseId}/original")
    public ResponseEntity<Resource> original(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable long courseId) {
        Resource resource = importService.original(user.userId(), courseId);
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resource.getFilename())
                .contentType(MediaType.parseMediaType(
                        CourseAudioFormat.fromFileName(resource.getFilename()).mediaType()))
                .body(resource);
    }

    /** 创建一条浏览器录音课程记录。 */
    @PostMapping
    public ResponseEntity<CourseResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CourseResponse.from(courseService.create(user.userId(), request.title(), request.lessonDate())));
    }

    /** 查询当前用户的课程记录列表。 */
    @GetMapping
    public List<CourseSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return courseService.list(user.userId()).stream().map(CourseSummaryResponse::from).toList();
    }

    /** 查询指定课程的转写原文、笔记和处理状态。 */
    @GetMapping("/{courseId}")
    public CourseResponse get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId) {
        return CourseResponse.from(courseService.get(user.userId(), courseId));
    }

    /** 修改指定课程的标题。 */
    @PutMapping("/{courseId}/title")
    public CourseTitleResponse rename(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId,
            @Valid @RequestBody UpdateCourseTitleRequest request) {
        return new CourseTitleResponse(courseService.rename(user.userId(), courseId, request.title()));
    }

    /** 轻量查询课程后台处理进度和失败原因。 */
    @GetMapping("/{courseId}/status")
    public CourseProgress status(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId) {
        return courseService.progress(user.userId(), courseId);
    }

    /** 上传浏览器录音产生的一个音频分片。 */
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

    /** 查询指定课程已经保存的全部录音分片。 */
    @GetMapping("/{courseId}/parts")
    public List<CourseAudioPartResponse> listParts(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId) {
        return courseAudioService.list(user.userId(), courseId).stream()
                .map(CourseAudioPartResponse::from)
                .toList();
    }

    /** 读取指定课程的一个 WebM 录音分片。 */
    @GetMapping(value = "/{courseId}/parts/{partNumber}/audio", produces = "audio/webm")
    public ResponseEntity<byte[]> readAudioPart(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId,
            @PathVariable int partNumber) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/webm"))
                .body(courseAudioService.read(user.userId(), courseId, partNumber));
    }

    /** 结束浏览器录音，并异步开始语音转写。 */
    @PostMapping("/{courseId}/complete")
    public ResponseEntity<CourseResponse> complete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId) {
        Course course = recordingService.complete(user.userId(), courseId);
        // HTTP 请求立即返回，耗时转写在线程池中执行。
        processingService.processAsync(user.userId(), courseId);
        return ResponseEntity.accepted().body(CourseResponse.from(course));
    }

    /** 从失败位置重新执行课程录音处理。 */
    @PostMapping("/{courseId}/retry")
    public ResponseEntity<CourseResponse> retry(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId) {
        boolean imported = "IMPORT".equals(courseService.get(user.userId(), courseId).sourceType());
        Course course = imported ? importService.retry(user.userId(), courseId)
                : recordingService.retry(user.userId(), courseId);
        if (imported) importProcessor.processAsync(user.userId(), courseId);
        else processingService.processAsync(user.userId(), courseId);
        return ResponseEntity.accepted().body(CourseResponse.from(course));
    }

    /** 根据已保存的转写原文异步生成课程笔记。 */
    @PostMapping("/{courseId}/note/generate")
    public ResponseEntity<CourseResponse> generateNote(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId) {
        Course course = courseService.beginNoteGeneration(user.userId(), courseId);
        processingService.generateNoteAsync(user.userId(), courseId);
        return ResponseEntity.accepted().body(CourseResponse.from(course));
    }

    /** 保存用户手动编辑后的课程笔记。 */
    @PutMapping("/{courseId}/note")
    public CourseResponse saveNote(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId,
            @Valid @RequestBody UpdateCourseNoteRequest request) {
        return CourseResponse.from(courseService.saveNote(user.userId(), courseId, request.noteContent()));
    }

    /** 对重新生成的候选笔记执行替换、追加或放弃操作。 */
    @PutMapping("/{courseId}/note/candidate")
    public CourseResponse resolveNoteCandidate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long courseId,
            @Valid @RequestBody ResolveNoteCandidateRequest request) {
        return CourseResponse.from(courseService.resolveNoteCandidate(user.userId(), courseId, request.action()));
    }

    /** 将已经完成的课程笔记导出为 DOCX 文件。 */
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
