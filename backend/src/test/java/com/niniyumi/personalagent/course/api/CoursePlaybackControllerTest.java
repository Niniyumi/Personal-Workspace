package com.niniyumi.personalagent.course.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.niniyumi.personalagent.course.application.CoursePlaybackService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;

class CoursePlaybackControllerTest {
    @TempDir Path directory;

    @Test
    void servesTheOriginalRecordingWithItsActualMediaType() throws Exception {
        CoursePlaybackService playback = mock(CoursePlaybackService.class);
        FileSystemResource audio = new FileSystemResource(
                Files.writeString(directory.resolve("original.mp3"), "audio"));
        when(playback.open("ticket")).thenReturn(audio);

        var response = new CoursePlaybackController(playback).play("ticket");

        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("audio/mpeg");
        assertThat(response.getBody()).isSameAs(audio);
    }
}
