package com.niniyumi.personalagent.weeklyreport.api;

import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import com.niniyumi.personalagent.weeklyreport.api.dto.WeeklyReportRequest;
import com.niniyumi.personalagent.weeklyreport.api.dto.WeeklyReportResponse;
import com.niniyumi.personalagent.weeklyreport.api.dto.DocxImportResponse;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportAiService;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportService;
import com.niniyumi.personalagent.weeklyreport.infrastructure.document.DocxTextExtractor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/weekly-reports")
public class WeeklyReportController {
    private final WeeklyReportService service;
    private final DocxTextExtractor extractor;
    private final WeeklyReportAiService aiService;

    public WeeklyReportController(
            WeeklyReportService service,
            DocxTextExtractor extractor,
            WeeklyReportAiService aiService) {
        this.service = service;
        this.extractor = extractor;
        this.aiService = aiService;
    }

    @PostMapping
    public ResponseEntity<WeeklyReportResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody WeeklyReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WeeklyReportResponse.from(service.create(user.userId(), request.toCommand())));
    }

    @GetMapping
    public List<WeeklyReportResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam int year,
            @RequestParam int month) {
        return service.list(user.userId(), year, month).stream()
                .map(WeeklyReportResponse::from)
                .toList();
    }

    @GetMapping("/{reportId}")
    public WeeklyReportResponse get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long reportId) {
        return WeeklyReportResponse.from(service.get(user.userId(), reportId));
    }

    @PutMapping("/{reportId}")
    public WeeklyReportResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long reportId,
            @Valid @RequestBody WeeklyReportRequest request) {
        return WeeklyReportResponse.from(service.update(user.userId(), reportId, request.toCommand()));
    }

    @PostMapping(value = "/import-docx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocxImportResponse importDocx(@RequestPart("file") MultipartFile file) {
        String text = extractor.extract(file);
        return DocxImportResponse.from(aiService.classifyDocument(text), file.getOriginalFilename());
    }
}
