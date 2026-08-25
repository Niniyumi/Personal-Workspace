package com.niniyumi.personalagent.course.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
import com.niniyumi.personalagent.course.application.CourseRecordingService;
import com.niniyumi.personalagent.course.application.CourseService;
import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseAudioPart;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.course.infrastructure.document.CourseDocxExporter;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = CourseController.class,
        properties = "app.security.jwt-secret=test-jwt-secret-for-course-tests-01")
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, GlobalExceptionHandler.class})
class CourseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @MockBean
    private CourseRecordingService recordingService;

    @MockBean
    private CourseProcessingService processingService;

    @MockBean
    private CourseDocxExporter docxExporter;

    @Test
    void requiresAuthenticationForCourseApis() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsAndListsOwnedCourses() throws Exception {
        when(courseService.create(42L, "Java 并发课")).thenReturn(course(CourseStatus.RECORDING));
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
    void retriesAFailedCourse() throws Exception {
        when(recordingService.retry(42L, 9L)).thenReturn(course(CourseStatus.PROCESSING));

        mockMvc.perform(post("/api/courses/9/retry").with(authentication(principalAuthentication())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
        verify(processingService).processAsync(42L, 9L);
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
