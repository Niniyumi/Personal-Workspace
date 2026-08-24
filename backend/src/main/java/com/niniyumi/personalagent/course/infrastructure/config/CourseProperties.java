package com.niniyumi.personalagent.course.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.course")
public record CourseProperties(String storageDir) {
}
