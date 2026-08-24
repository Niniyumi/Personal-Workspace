package com.niniyumi.personalagent.weeklyreport.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.niniyumi.personalagent.auth.infrastructure.security.ApiSecurityErrorHandler;
import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import com.niniyumi.personalagent.auth.infrastructure.security.SecurityConfig;
import com.niniyumi.personalagent.common.api.GlobalExceptionHandler;
import com.niniyumi.personalagent.weeklyreport.application.NoWeeklyReportsForPeriodException;
import com.niniyumi.personalagent.weeklyreport.application.WorkSummaryService;
import com.niniyumi.personalagent.weeklyreport.domain.SummaryPeriodType;
import com.niniyumi.personalagent.weeklyreport.domain.WorkSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = WorkSummaryController.class,
        properties = "app.security.jwt-secret=test-jwt-secret-for-webmvc-tests-01")
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, GlobalExceptionHandler.class})
class WorkSummaryControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private WorkSummaryService service;

    @Test
    void generatesQuarterAndReturnsTheThreeModules() throws Exception {
        when(service.generateQuarter(42L, 2026, 3)).thenReturn(summary());

        mockMvc.perform(post("/api/work-summaries/generate")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"periodType":"QUARTER","year":2026,"quarter":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coreContent").value("核心成果"))
                .andExpect(jsonPath("$.routineWork").value("日常维护"))
                .andExpect(jsonPath("$.selfScore").value(88));
    }

    @Test
    void getsASelectedPeriod() throws Exception {
        when(service.get(42L, SummaryPeriodType.QUARTER,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30))).thenReturn(summary());

        mockMvc.perform(get("/api/work-summaries?periodType=QUARTER&year=2026&quarter=3")
                        .with(authentication(principalAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void rejectsInvalidPeriodAndScore() throws Exception {
        mockMvc.perform(post("/api/work-summaries/generate")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"periodType":"QUARTER","year":2026}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/work-summaries/7")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"coreContent":"核心","routineWork":"日常","selfScore":101}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyPeriodHasAStableConflictCode() throws Exception {
        when(service.generateYear(42L, 2026)).thenThrow(new NoWeeklyReportsForPeriodException());

        mockMvc.perform(post("/api/work-summaries/generate")
                        .with(authentication(principalAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"periodType":"YEAR","year":2026}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_WEEKLY_REPORTS_FOR_PERIOD"));
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/work-summaries?periodType=YEAR&year=2026"))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken principalAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(42L, "nini"), "token", List.of());
    }

    private WorkSummary summary() {
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        return new WorkSummary(7L, 42L, SummaryPeriodType.QUARTER,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30),
                "核心成果", "日常维护", 88, now, now, now);
    }
}
