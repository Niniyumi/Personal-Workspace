package com.niniyumi.personalagent.course.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.niniyumi.personalagent.course.infrastructure.config.CourseProperties;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CourseImportStorageTest {
    @TempDir Path root;

    @Test
    void appendsAtExpectedOffsetAndCanResume() throws Exception {
        CourseImportStorage storage = new CourseImportStorage(new CourseProperties(root.toString()));
        assertThat(storage.append(42, 9, 0, new ByteArrayInputStream("abc".getBytes()), 6)).isEqualTo(3);
        assertThat(storage.offset(42, 9)).isEqualTo(3);
        assertThat(storage.append(42, 9, 3, new ByteArrayInputStream("def".getBytes()), 6)).isEqualTo(6);
        Path original = storage.finish(42, 9, 6, storage.original(42, 9, "mp3"));
        assertThat(Files.readString(original)).isEqualTo("abcdef");
        assertThat(original.getFileName().toString()).isEqualTo("original.mp3");
    }

    @Test
    void rejectsWrongOffsetWithoutChangingSavedFile() {
        CourseImportStorage storage = new CourseImportStorage(new CourseProperties(root.toString()));
        storage.append(42, 9, 0, new ByteArrayInputStream("abc".getBytes()), 6);
        assertThatThrownBy(() -> storage.append(42, 9, 0, new ByteArrayInputStream("def".getBytes()), 6))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(storage.offset(42, 9)).isEqualTo(3);
    }

    @Test
    void rejectsOversizedChunkBeforeWriting() {
        CourseImportStorage storage = new CourseImportStorage(new CourseProperties(root.toString()));
        assertThatThrownBy(() -> storage.append(42, 9, 0,
                new ByteArrayInputStream(new byte[8 * 1024 * 1024 + 1]), 9 * 1024 * 1024L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(storage.offset(42, 9)).isZero();
    }
}
