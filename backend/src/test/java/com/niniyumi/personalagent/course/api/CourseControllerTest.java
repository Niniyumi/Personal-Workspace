package com.niniyumi.personalagent.course.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.niniyumi.personalagent.auth.infrastructure.security.ApiSecurityErrorHandler;
import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import com.niniyumi.personalagent.auth.infrastructure.security.SecurityConfig;
import com.niniyumi.personalagent.common.api.GlobalExceptionHandler;
import com.niniyumi.personalagent.course.application.CourseProcessingService;
import com.niniyumi.personalagent.course.application.CourseAudioService;
import com.niniyumi.personalagent.course.application.CourseRecordingService;
import com.niniyumi.personalagent.course.application.CourseService;
import com.niniyumi.personalagent.course.application.CourseImportService;
import com.niniyumi.personalagent.course.application.CourseImportProcessor;
import com.niniyumi.personalagent.course.application.CoursePlaybackService;
import com.niniyumi.personalagent.course.domain.CourseProgress;
import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseAudioPart;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.infrastructure.document.CourseDocxExporter;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = CourseController.class,
        properties = "app.security.jwt-secret=test-jwt-secret-for-course-tests-01")
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, GlobalExceptionHandler.class})
class CourseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @TempDir
    Path audioDirectory;

    @MockBean
    private CourseService courseService;

    @MockBean
    private CourseRecordingService recordingService;

    @MockBean
    private CourseProcessingService processingService;

    @MockBean
    private CourseDocxExporter docxExporter;

    @MockBean
    private CourseAudioService courseAudioService;

    @MockBean
    private CourseImportService importService;

    @MockBean
    private CourseImportProcessor importProcessor;

    @MockBean
    private CoursePlaybackService playbackService;

    @Test
    void createsAndCompletesAuthenticatedAudioImport() throws Exception {
        Course uploading = new Course(9L, 42L, "网络课", CourseStatus.UPLOADING, 0, 0,
                null, null, null, "IMPORT", null, 6L,
                Instant.parse("2026-08-24T12:00:00Z"), Instant.parse("2026-08-24T12:00:00Z"));
        Course processing = new Course(9L, 42L, "网络课", CourseStatus.PROCESSING, 0, 0,
                null, null, null, "IMPORT", "audio/original.m4a", 6L,
                Instant.parse("2026-08-24T12:00:00Z"), Instant.parse("2026-08-24T12:00:00Z"));
        when(importService.create(42L, "网络课", 6L, "lecture.mp3", null)).thenReturn(uploading);
        when(importService.append(anyLong(), anyLong(), anyLong(), any())).thenReturn(6L);
        when(importService.finish(42L, 9L)).thenReturn(processing);

        mockMvc.perform(post("/api/courses/imports")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"网络课\",\"fileSize\":6,\"fileName\":\"lecture.mp3\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UPLOADING"));
        mockMvc.perform(put("/api/courses/imports/9/chunk?offset=0")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("abcdef"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offset").value(6));
        mockMvc.perform(post("/api/courses/imports/9/complete")
                        .with(authentication(principalAuthentication())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
        verify(importProcessor).processAsync(42L, 9L);
    }

    @Test
    void rejectsAnImportedCourseTitleLongerThanTheDatabaseLimit() throws Exception {
        mockMvc.perform(post("/api/courses/imports")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "课".repeat(161)
                                + "\",\"fileSize\":6,\"fileName\":\"lecture.mp3\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(importService);
    }

    @Test
    void streamsOnlyTheRequestedBytesOfTheOriginalRecording() throws Exception {
        Path audio = Files.writeString(audioDirectory.resolve("original.mp3"), "abcdef");
        when(importService.original(42L, 9L)).thenReturn(new FileSystemResource(audio));

        mockMvc.perform(get("/api/courses/9/original")
                        .with(authentication(principalAuthentication()))
                        .header(HttpHeaders.RANGE, "bytes=2-4"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 2-4/6"))
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().string("cde"));
    }

    @Test
    void requiresAuthenticationForCourseApis() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsAndListsOwnedCourses() throws Exception {
        when(courseService.create(42L, "Java 并发课", null)).thenReturn(course(CourseStatus.RECORDING));
        when(courseService.list(42L)).thenReturn(List.of(course(CourseStatus.RECORDING)));

        mockMvc.perform(post("/api/courses")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Java 并发课\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.status").value("RECORDING"));

        mockMvc.perform(get("/api/courses").with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Java 并发课"))
                .andExpect(jsonPath("$[0].transcript").doesNotExist())
                .andExpect(jsonPath("$[0].noteContent").doesNotExist());
    }

    @Test
    void uploadsAPartAndStartsProcessingOnComplete() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "part.webm", "audio/webm", "audio".getBytes());
        when(recordingService.uploadPart(42L, 9L, 1, 20, file))
                .thenReturn(new CourseAudioPart(3L, 9L, 1, 20, "audio/9/1.webm", 5,
                        Instant.parse("2026-08-24T12:00:00Z")));
        when(recordingService.complete(42L, 9L)).thenReturn(course(CourseStatus.PROCESSING));

        mockMvc.perform(multipart("/api/courses/9/parts")
                        .file(file)
                        .param("partNumber", "1")
                        .param("durationSeconds", "20")
                        .with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partNumber").value(1));

        mockMvc.perform(post("/api/courses/9/complete").with(authentication(principalAuthentication())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
        verify(processingService).processAsync(42L, 9L);
    }

    @Test
    void readsAndSavesAnEditedNote() throws Exception {
        when(courseService.get(42L, 9L)).thenReturn(course(CourseStatus.READY));
        when(courseService.saveNote(42L, 9L, "# 我的笔记")).thenReturn(course(CourseStatus.READY));

        mockMvc.perform(get("/api/courses/9").with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transcript").value("完整转写"));

        mockMvc.perform(put("/api/courses/9/note")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noteContent\":\"# 我的笔记\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noteContent").value("# 我的笔记"));
    }

    @Test
    void renamesAnOwnedCourse() throws Exception {
        when(courseService.rename(42L, 9L, "新笔记名称")).thenReturn("新笔记名称");

        mockMvc.perform(put("/api/courses/9/title")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"新笔记名称\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("新笔记名称"));
    }

    @Test
    void retriesAFailedCourse() throws Exception {
        when(courseService.get(42L, 9L)).thenReturn(course(CourseStatus.FAILED));
        when(recordingService.retry(42L, 9L)).thenReturn(course(CourseStatus.PROCESSING));

        mockMvc.perform(post("/api/courses/9/retry").with(authentication(principalAuthentication())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
        verify(processingService).processAsync(42L, 9L);
    }

    @Test
    void returnsOnlyLightweightProcessingStatusForPolling() throws Exception {
        when(courseService.progress(42L, 9L))
                .thenReturn(new CourseProgress(CourseStatus.PROCESSING, 65, null));

        mockMvc.perform(get("/api/courses/9/status").with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.processingProgress").value(65))
                .andExpect(jsonPath("$.transcript").doesNotExist())
                .andExpect(jsonPath("$.noteContent").doesNotExist());
    }

    @Test
    void retriesAnImportedCourseThroughTheImportProcessor() throws Exception {
        Course imported = new Course(9L, 42L, "网络课", CourseStatus.FAILED, 300, 43,
                "第一段", null, "转写失败", "IMPORT", "audio/original.m4a", 100L,
                Instant.parse("2026-08-24T12:00:00Z"), Instant.parse("2026-08-24T12:00:00Z"));
        when(courseService.get(42L, 9L)).thenReturn(imported);
        when(importService.retry(42L, 9L)).thenReturn(imported);

        mockMvc.perform(post("/api/courses/9/retry").with(authentication(principalAuthentication())))
                .andExpect(status().isAccepted());
        verify(importProcessor).processAsync(42L, 9L);
    }

    @Test
    void startsNoteGenerationOnlyAfterTheTranscriptIsSaved() throws Exception {
        Course transcribed = course(CourseStatus.TRANSCRIBED);
        Course processing = new Course(
                transcribed.id(), transcribed.userId(), transcribed.title(), CourseStatus.PROCESSING,
                transcribed.durationSeconds(), 85, transcribed.transcript(), null, null,
                transcribed.createdAt(), transcribed.updatedAt());
        when(courseService.beginNoteGeneration(42L, 9L)).thenReturn(processing);

        mockMvc.perform(post("/api/courses/9/note/generate")
                        .with(authentication(principalAuthentication())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.transcript").value("完整转写"));

        verify(processingService).generateNoteAsync(42L, 9L);
    }

    @Test
    void listsAndStreamsOwnedAudioParts() throws Exception {
        CourseAudioPart part = new CourseAudioPart(3L, 9L, 1, 20, "audio/9/1.webm", 5,
                Instant.parse("2026-08-24T12:00:00Z"));
        when(courseAudioService.list(42L, 9L)).thenReturn(List.of(part));
        when(courseAudioService.read(42L, 9L, 1)).thenReturn("audio".getBytes());

        mockMvc.perform(get("/api/courses/9/parts").with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partNumber").value(1));

        mockMvc.perform(get("/api/courses/9/parts/1/audio")
                        .with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/webm"))
                .andExpect(content().bytes("audio".getBytes()));
    }

    @Test
    void downloadsAnOwnedCourseNoteAsDocx() throws Exception {
        when(courseService.get(42L, 9L)).thenReturn(course(CourseStatus.READY));
        when(docxExporter.export(any(Course.class))).thenReturn("docx".getBytes());

        mockMvc.perform(get("/api/courses/9/note.docx").with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().bytes("docx".getBytes()));
    }

    private UsernamePasswordAuthenticationToken principalAuthentication() {
        return new UsernamePasswordAuthenticationToken(new AuthenticatedUser(42L, "nini"), "token", List.of());
    }

    private Course course(CourseStatus status) {
        return new Course(9L, 42L, "Java 并发课", status, 20,
                status == CourseStatus.READY ? 100 : 10, "完整转写", "# 我的笔记", null,
                Instant.parse("2026-08-24T12:00:00Z"), Instant.parse("2026-08-24T12:01:00Z"));
    }
}
