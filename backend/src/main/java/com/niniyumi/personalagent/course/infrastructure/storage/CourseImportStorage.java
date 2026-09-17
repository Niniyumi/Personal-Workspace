package com.niniyumi.personalagent.course.infrastructure.storage;

import com.niniyumi.personalagent.course.infrastructure.config.CourseProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.stereotype.Component;

@Component
public class CourseImportStorage {
    public static final int MAX_CHUNK_BYTES = 8 * 1024 * 1024;
    private final Path root;

    public CourseImportStorage(CourseProperties properties) {
        root = Path.of(properties.storageDir()).toAbsolutePath().normalize();
    }

    public long offset(long userId, long courseId) {
        Path upload = directory(userId, courseId).resolve("original.upload");
        try {
            return Files.exists(upload) ? Files.size(upload) : 0;
        } catch (IOException exception) {
            throw new CourseStorageException(exception);
        }
    }

    public long append(long userId, long courseId, long offset, InputStream content, long expectedBytes) {
        try {
            byte[] chunk = content.readNBytes(MAX_CHUNK_BYTES + 1);
            if (chunk.length == 0 || chunk.length > MAX_CHUNK_BYTES
                    || offset != offset(userId, courseId) || offset + chunk.length > expectedBytes) {
                throw new IllegalArgumentException("Invalid audio upload chunk");
            }
            Path directory = directory(userId, courseId);
            Files.createDirectories(directory);
            Files.write(directory.resolve("original.upload"), chunk,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return offset + chunk.length;
        } catch (IOException exception) {
            throw new CourseStorageException(exception);
        }
    }

    public Path finish(long userId, long courseId, long expectedBytes, Path original) {
        if (offset(userId, courseId) != expectedBytes) {
            throw new IllegalArgumentException("Audio upload is incomplete");
        }
        try {
            return Files.move(directory(userId, courseId).resolve("original.upload"), original);
        } catch (IOException exception) {
            throw new CourseStorageException(exception);
        }
    }

    public Path original(long userId, long courseId, String extension) {
        return directory(userId, courseId).resolve("original." + extension);
    }

    public Path uploadPath(long userId, long courseId) {
        return directory(userId, courseId).resolve("original.upload");
    }

    public Path segmentDirectory(long userId, long courseId) {
        return directory(userId, courseId);
    }

    private Path directory(long userId, long courseId) {
        return root.resolve(Long.toString(userId)).resolve(Long.toString(courseId));
    }
}
