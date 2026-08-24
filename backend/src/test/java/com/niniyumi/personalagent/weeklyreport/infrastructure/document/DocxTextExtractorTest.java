package com.niniyumi.personalagent.weeklyreport.infrastructure.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.niniyumi.personalagent.weeklyreport.application.InvalidDocxException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DocxTextExtractorTest {
    private final DocxTextExtractor extractor = new DocxTextExtractor();

    @Test
    void extractsParagraphsAndTableCells() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "week-34.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                documentBytes(true));

        assertThat(extractor.extract(file))
                .contains("完成登录模块", "修复接口问题", "下周实现周报", "表格内容");
    }

    @Test
    void rejectsEmptyWrongExtensionAndInvalidDocxFiles() throws IOException {
        assertThatThrownBy(() -> extractor.extract(
                new MockMultipartFile("file", "empty.docx", "application/octet-stream", new byte[0])))
                .isInstanceOf(InvalidDocxException.class);
        assertThatThrownBy(() -> extractor.extract(
                new MockMultipartFile("file", "week.txt", "text/plain", documentBytes(true))))
                .isInstanceOf(InvalidDocxException.class);
        assertThatThrownBy(() -> extractor.extract(
                new MockMultipartFile("file", "broken.docx", "application/octet-stream", "not-docx".getBytes())))
                .isInstanceOf(InvalidDocxException.class);
        assertThatThrownBy(() -> extractor.extract(
                new MockMultipartFile("file", "blank.docx", "application/octet-stream", documentBytes(false))))
                .isInstanceOf(InvalidDocxException.class);
    }

    @Test
    void rejectsFilesOverTenMegabytesBeforeParsing() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.docx", "application/octet-stream", new byte[10 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOf(InvalidDocxException.class);
    }

    private byte[] documentBytes(boolean withContent) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (withContent) {
                document.createParagraph().createRun().setText("完成登录模块");
                document.createParagraph().createRun().setText("修复接口问题\n下周实现周报");
                document.createTable(1, 1).getRow(0).getCell(0).setText("表格内容");
            }
            document.write(output);
            return output.toByteArray();
        }
    }
}
