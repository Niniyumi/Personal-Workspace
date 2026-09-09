package com.niniyumi.personalagent.weeklyreport.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.niniyumi.personalagent.auth.infrastructure.security.ApiSecurityErrorHandler;
import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import com.niniyumi.personalagent.auth.infrastructure.security.SecurityConfig;
import com.niniyumi.personalagent.common.api.GlobalExceptionHandler;
import com.niniyumi.personalagent.weeklyreport.application.InvalidWeekStartException;
import com.niniyumi.personalagent.weeklyreport.application.DocumentClassification;
import com.niniyumi.personalagent.weeklyreport.application.InvalidDocxException;
import com.niniyumi.personalagent.weeklyreport.application.SaveWeeklyReportCommand;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportAlreadyExistsException;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportNotFoundException;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportService;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportDateResolver;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportAiService;
import com.niniyumi.personalagent.weeklyreport.infrastructure.document.DocxTextExtractor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

@WebMvcTest(value = WeeklyReportController.class,
        properties = "app.security.jwt-secret=test-jwt-secret-for-webmvc-tests-01")
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, GlobalExceptionHandler.class})
@ExtendWith(OutputCaptureExtension.class)
class WeeklyReportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeeklyReportService service;

    @MockBean
    private DocxTextExtractor extractor;

    @MockBean
    private WeeklyReportAiService aiService;

    @MockBean
    private WeeklyReportDateResolver dateResolver;

    @Test
    void createReturnsTheOwnedReport() throws Exception {
        when(service.create(eq(42L), any(SaveWeeklyReportCommand.class))).thenReturn(report());

        mockMvc.perform(post("/api/weekly-reports")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.weekStartDate").value("2026-08-24"))
                .andExpect(jsonPath("$.coreWork").value("完成登录"));
    }

    @Test
    void listReturnsTheSelectedMonth() throws Exception {
        when(service.list(42L, 2026, 8)).thenReturn(List.of(report()));

        mockMvc.perform(get("/api/weekly-reports?year=2026&month=8")
                        .with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9));
    }

    @Test
    void getReturnsAReportDetail() throws Exception {
        when(service.get(42L, 9L)).thenReturn(report());

        mockMvc.perform(get("/api/weekly-reports/9")
                        .with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextWeekPlan").value("开发周报"));
    }

    @Test
    void updateReturnsTheSavedReport() throws Exception {
        when(service.update(eq(42L), eq(9L), any(SaveWeeklyReportCommand.class))).thenReturn(report());

        mockMvc.perform(put("/api/weekly-reports/9")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void blankCoreWorkReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/weekly-reports")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weekStartDate":"2026-08-24","coreWork":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/weekly-reports?year=2026&month=8"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void importDocxClassifiesTextWithoutSavingAReport(CapturedOutput output) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "week-34.docx", "application/octet-stream", "docx".getBytes());
        when(extractor.extract(any())).thenReturn("原始周报文字");
        when(aiService.classifyDocument("原始周报文字"))
                .thenReturn(new DocumentClassification("完成登录", "接口超时", "开发周报"));
        when(dateResolver.resolveWeekStart("week-34.docx", "原始周报文字"))
                .thenReturn(java.util.Optional.of(LocalDate.of(2026, 8, 24)));

        mockMvc.perform(multipart("/api/weekly-reports/import-docx")
                        .file(file)
                        .header("X-Batch-Index", "2")
                        .header("X-Batch-Total", "5")
                        .with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coreWork").value("完成登录"))
                .andExpect(jsonPath("$.problems").value("接口超时"))
                .andExpect(jsonPath("$.nextWeekPlan").value("开发周报"))
                .andExpect(jsonPath("$.weekStartDate").value("2026-08-24"))
                .andExpect(jsonPath("$.sourceFileName").value("week-34.docx"));
        assertThat(output)
                .contains("Weekly report DOCX extracted, batch=2/5, fileBytes=4, textChars=6")
                .contains("Weekly report DOCX classified, batch=2/5, weekStart=2026-08-24");
        verifyNoInteractions(service);
    }

    @Test
    void invalidDocxReturnsAStableError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "broken.docx", "application/octet-stream", "broken".getBytes());
        when(extractor.extract(any())).thenThrow(new InvalidDocxException());

        mockMvc.perform(multipart("/api/weekly-reports/import-docx")
                        .file(file)
                        .with(authentication(principalAuthentication())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DOCX"));
    }

    @Test
    void domainErrorsHaveStableApiCodes() throws Exception {
        when(service.get(42L, 9L)).thenThrow(new WeeklyReportNotFoundException());
        when(service.create(eq(42L), any())).thenThrow(new WeeklyReportAlreadyExistsException());
        when(service.update(eq(42L), eq(9L), any())).thenThrow(new InvalidWeekStartException());

        mockMvc.perform(get("/api/weekly-reports/9").with(authentication(principalAuthentication())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WEEKLY_REPORT_NOT_FOUND"));
        mockMvc.perform(post("/api/weekly-reports")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WEEKLY_REPORT_EXISTS"));
        mockMvc.perform(put("/api/weekly-reports/9")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WEEK_START"));
    }

    private UsernamePasswordAuthenticationToken principalAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(42L, "nini"), "token", List.of());
    }

    private String validRequest() {
        return """
                {
                  "weekStartDate":"2026-08-24",
                  "coreWork":"完成登录",
                  "problems":"接口超时",
                  "nextWeekPlan":"开发周报"
                }
                """;
    }

    private WeeklyReport report() {
        return new WeeklyReport(9L, 42L, LocalDate.of(2026, 8, 24), "完成登录", "接口超时",
                "开发周报", null, Instant.parse("2026-08-24T08:00:00Z"),
                Instant.parse("2026-08-24T10:00:00Z"));
    }
}
