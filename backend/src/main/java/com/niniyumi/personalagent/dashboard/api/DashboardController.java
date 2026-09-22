package com.niniyumi.personalagent.dashboard.api;

import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import com.niniyumi.personalagent.dashboard.application.DashboardService;
import com.niniyumi.personalagent.dashboard.application.DashboardSnapshot;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    /** 查询当前用户首页所需的统计数据和最近记录。 */
    @GetMapping
    public DashboardSnapshot get(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.get(user.userId());
    }
}
