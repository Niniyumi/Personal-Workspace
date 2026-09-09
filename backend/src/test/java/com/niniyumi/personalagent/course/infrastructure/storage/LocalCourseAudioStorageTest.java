package com.niniyumi.personalagent.course.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.niniyumi.personalagent.course.infrastructure.config.CourseProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalCourseAudioStorageTest {
    @TempDir
    Path directory;

    @Test
    void readsPreviouslyStoredAudio() {
        LocalCourseAudioStorage storage = new LocalCourseAudioStorage(
                new CourseProperties(directory.toString()));
        Path stored = storage.store(42L, 9L, 1,
                new MockMultipartFile("file", "part.webm", "audio/webm", "audio".getBytes()));

        assertThat(storage.read(stored)).isEqualTo("audio".getBytes());
        assertThat(Files.exists(stored)).isTrue();
    }
}
