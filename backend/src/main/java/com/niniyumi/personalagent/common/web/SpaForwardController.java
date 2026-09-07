package com.niniyumi.personalagent.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({"/", "/login", "/register", "/forgot-password", "/weekly-reports", "/weekly-reports/{id}",
            "/work-summaries", "/courses", "/courses/{id}"})
    public String forwardSpaRoutes() {
        // 只转发已知的前端页面，避免覆盖 /api 或静态资源请求。
        return "forward:/index.html";
    }
}
