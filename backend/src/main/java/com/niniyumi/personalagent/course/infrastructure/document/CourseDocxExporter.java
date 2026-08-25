package com.niniyumi.personalagent.course.infrastructure.document;

import com.niniyumi.personalagent.course.domain.Course;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.stereotype.Component;

@Component
public class CourseDocxExporter {
    private static final String FONT = "Microsoft YaHei";
    private static final String ACCENT = "F05A18";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public byte[] export(Course course) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configurePage(document);
            addTitle(document, course.title());
            addMetadata(document, course);
            addSectionHeading(document, "课程笔记");
            addNote(document, course.noteContent());
            addSectionHeading(document, "完整转写");
            addBody(document, course.transcript());
            document.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("课程笔记 DOCX 生成失败", exception);
        }
    }

    private void configurePage(XWPFDocument document) {
        CTSectPr section = document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = section.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(12240));
        pageSize.setH(BigInteger.valueOf(15840));
        CTPageMar margins = section.addNewPgMar();
        margins.setTop(BigInteger.valueOf(1440));
        margins.setRight(BigInteger.valueOf(1440));
        margins.setBottom(BigInteger.valueOf(1440));
        margins.setLeft(BigInteger.valueOf(1440));
        margins.setHeader(BigInteger.valueOf(708));
        margins.setFooter(BigInteger.valueOf(708));
    }

    private void addTitle(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setSpacingAfter(120);
        XWPFRun run = paragraph.createRun();
        run.setText(title);
        style(run, 26, true, "17181C");
    }

    private void addMetadata(XWPFDocument document, Course course) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(360);
        XWPFRun run = paragraph.createRun();
        int minutes = Math.max(1, (int) Math.ceil(course.durationSeconds() / 60D));
        run.setText("课程时长 " + minutes + " 分钟  |  生成时间 " + DATE_TIME.format(course.updatedAt()));
        style(run, 10, false, "77736C");
    }

    private void addSectionHeading(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(280);
        paragraph.setSpacingAfter(140);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        style(run, 16, true, ACCENT);
    }

    private void addNote(XWPFDocument document, String content) {
        if (content == null || content.isBlank()) {
            addParagraph(document, "暂无课程笔记", false);
            return;
        }
        for (String line : content.split("\\R")) {
            String value = line.trim();
            if (value.isEmpty()) continue;
            if (value.startsWith("### ")) {
                addSubheading(document, value.substring(4));
            } else if (value.startsWith("## ")) {
                addSubheading(document, value.substring(3));
            } else if (value.startsWith("# ")) {
                addSubheading(document, value.substring(2));
            } else {
                addParagraph(document, value.startsWith("- ") ? value.substring(2) : value, value.startsWith("- "));
            }
        }
    }

    private void addSubheading(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(180);
        paragraph.setSpacingAfter(100);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        style(run, 13, true, "24262D");
    }

    private void addBody(XWPFDocument document, String content) {
        if (content == null || content.isBlank()) {
            addParagraph(document, "暂无转写内容", false);
            return;
        }
        for (String block : content.split("\\R\\s*\\R")) {
            addParagraph(document, block.trim(), false);
        }
    }

    private void addParagraph(XWPFDocument document, String text, boolean indent) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(120);
        paragraph.setSpacingBetween(1.25);
        if (indent) paragraph.setIndentationLeft(540);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        style(run, 11, false, "333333");
    }

    private void style(XWPFRun run, int size, boolean bold, String color) {
        run.setFontFamily(FONT);
        run.setFontSize(size);
        run.setBold(bold);
        run.setColor(color);
    }
}
