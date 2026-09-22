package com.niniyumi.personalagent.weeklyreport.api;

import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import com.niniyumi.personalagent.weeklyreport.api.dto.WeeklyReportRequest;
import com.niniyumi.personalagent.weeklyreport.api.dto.WeeklyReportResponse;
import com.niniyumi.personalagent.weeklyreport.api.dto.WeeklyReportSearchResponse;
import com.niniyumi.personalagent.weeklyreport.api.dto.DocxImportResponse;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportAiService;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportService;
import com.niniyumi.personalagent.weeklyreport.application.WeeklyReportDateResolver;
import com.niniyumi.personalagent.weeklyreport.infrastructure.document.DocxTextExtractor;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/weekly-reports")
public class WeeklyReportController {
    private static final Logger log = LoggerFactory.getLogger(WeeklyReportController.class);
    private final WeeklyReportService service;
    private final DocxTextExtractor extractor;
    private final WeeklyReportAiService aiService;
    private final WeeklyReportDateResolver dateResolver;

    public WeeklyReportController(
            WeeklyReportService service,
            DocxTextExtractor extractor,
            WeeklyReportAiService aiService,
            WeeklyReportDateResolver dateResolver) {
        this.service = service;
        this.extractor = extractor;
        this.aiService = aiService;
        this.dateResolver = dateResolver;
    }

    /** 创建指定自然周的周报。 */
    @PostMapping
    public ResponseEntity<WeeklyReportResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody WeeklyReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WeeklyReportResponse.from(service.create(user.userId(), request.toCommand())));
    }

    /** 按年份和月份查询当前用户的周报列表。 */
    @GetMapping
    public List<WeeklyReportResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam int year,
            @RequestParam int month) {
        return service.list(user.userId(), year, month).stream()
                .map(WeeklyReportResponse::from)
                .toList();
    }

    /** 查询指定周报的完整内容。 */
    @GetMapping("/{reportId}")
    public WeeklyReportResponse get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long reportId) {
        return WeeklyReportResponse.from(service.get(user.userId(), reportId));
    }

    /** 修改指定周报的日期和工作内容。 */
    @PutMapping("/{reportId}")
    public WeeklyReportResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long reportId,
            @Valid @RequestBody WeeklyReportRequest request) {
        return WeeklyReportResponse.from(service.update(user.userId(), reportId, request.toCommand()));
    }

    /** 从 DOCX 中提取文字、识别周日期并生成可编辑的周报预览。 */
    @PostMapping(value = "/import-docx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocxImportResponse importDocx(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Batch-Index", defaultValue = "1") int batchIndex,
            @RequestHeader(value = "X-Batch-Total", defaultValue = "1") int batchTotal) {
        String fileName = file.getOriginalFilename();
        log.info("开始上传周报, batch={}/{}, fileName={}, fileBytes={}",
                batchIndex, batchTotal, fileName, file.getSize());
        String stage = "提取文字";
        try {
            String text = extractor.extract(file);
            log.info("Weekly report DOCX extracted, batch={}/{}, fileBytes={}, textChars={}",
                    batchIndex, batchTotal, file.getSize(), text.length());
            stage = "识别内容";
            DocxImportResponse response = DocxImportResponse.from(
                    aiService.classifyDocument(text), fileName,
                    dateResolver.resolveWeekStart(fileName, text).orElse(null));
            log.info("周报上传识别成功, batch={}/{}, fileName={}, weekStart={}",
                    batchIndex, batchTotal, fileName, response.weekStartDate());
            return response;
        } catch (RuntimeException exception) {
            log.warn("周报上传识别失败, batch={}/{}, fileName={}, fileBytes={}, stage={}, exceptionType={}",
                    batchIndex, batchTotal, fileName, file.getSize(), stage,
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    /** 按所属周筛选，并按内容检索当前用户的周报。 */
    @GetMapping("/search")
    public WeeklyReportSearchResponse search(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam int year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page) {
        return WeeklyReportSearchResponse.from(service.search(user.userId(), year, month, keyword, page));
    }
}
