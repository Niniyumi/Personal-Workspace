package com.niniyumi.personalagent.course.api;

import com.niniyumi.personalagent.course.application.CoursePlaybackService;
import com.niniyumi.personalagent.course.infrastructure.media.CourseAudioFormat;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course-audio")
public class CoursePlaybackController {
    private final CoursePlaybackService playback;

    public CoursePlaybackController(CoursePlaybackService playback) {
        this.playback = playback;
    }

    /** 使用短期播放凭证读取课程原始录音。 */
    @GetMapping("/{ticket}")
    public ResponseEntity<Resource> play(@PathVariable String ticket) {
        Resource resource = playback.open(ticket);
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(MediaType.parseMediaType(
                        CourseAudioFormat.fromFileName(resource.getFilename()).mediaType()))
                .body(resource);
    }
}
