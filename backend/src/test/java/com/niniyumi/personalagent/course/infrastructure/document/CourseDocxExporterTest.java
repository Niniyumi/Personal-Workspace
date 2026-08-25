package com.niniyumi.personalagent.course.infrastructure.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class CourseDocxExporterTest {
    @Test
    void exportsCourseMetadataNotesAndTranscript() throws Exception {
        Course course = new Course(9L, 42L, "Java 并发课", CourseStatus.READY, 120, 100,
                "线程与进程的区别", "# 课程摘要\n理解线程池的核心参数", null,
                Instant.parse("2026-08-24T12:00:00Z"), Instant.parse("2026-08-24T12:01:00Z"));

        byte[] bytes = new CourseDocxExporter().export(course);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", (left, right) -> left + "\n" + right);
            assertThat(text).contains("Java 并发课", "课程笔记", "理解线程池的核心参数", "完整转写", "线程与进程的区别");
        }
    }
}
