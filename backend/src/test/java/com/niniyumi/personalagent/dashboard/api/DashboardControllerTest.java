package com.niniyumi.personalagent.dashboard.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.niniyumi.personalagent.auth.infrastructure.security.ApiSecurityErrorHandler;
import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import com.niniyumi.personalagent.auth.infrastructure.security.SecurityConfig;
import com.niniyumi.personalagent.common.api.GlobalExceptionHandler;
import com.niniyumi.personalagent.dashboard.application.DashboardService;
import com.niniyumi.personalagent.dashboard.application.DashboardSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = DashboardController.class,
        properties = "app.security.jwt-secret=test-jwt-secret-for-dashboard-01")
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, GlobalExceptionHandler.class})
class DashboardControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean DashboardService dashboardService;

    @Test
    void returnsOnlyTheAuthenticatedUsersDashboard() throws Exception {
        DashboardSnapshot snapshot = new DashboardSnapshot(
                4, 2, 900, 1, 1, 600, List.of(), List.of(), List.of());
        when(dashboardService.get(42L)).thenReturn(snapshot);

        mockMvc.perform(get("/api/dashboard").with(authentication(new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(42L, "nini"), "token", List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWeeklyReports").value(4))
                .andExpect(jsonPath("$.totalCourseNotes").value(2));
    }
}
