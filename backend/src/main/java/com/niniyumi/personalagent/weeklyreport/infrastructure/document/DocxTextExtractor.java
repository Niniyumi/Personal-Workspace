package com.niniyumi.personalagent.weeklyreport.infrastructure.document;

import com.niniyumi.personalagent.weeklyreport.application.InvalidDocxException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocxTextExtractor {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    public String extract(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (file.isEmpty()
                || file.getSize() > MAX_FILE_SIZE
                || fileName == null
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            throw new InvalidDocxException();
        }

        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            List<String> parts = new ArrayList<>();
            document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .forEach(parts::add);
            document.getTables().forEach(table -> table.getRows().forEach(row ->
                    row.getTableCells().stream()
                            .map(XWPFTableCell::getText)
                            .map(String::trim)
                            .filter(text -> !text.isEmpty())
                            .forEach(parts::add)));

            if (parts.isEmpty()) {
                throw new InvalidDocxException();
            }
            return String.join("\n", parts);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof InvalidDocxException invalidDocxException) {
                throw invalidDocxException;
            }
            throw new InvalidDocxException(exception);
        }
    }
}
