package com.niniyumi.personalagent.course.infrastructure.storage;

import com.niniyumi.personalagent.course.infrastructure.config.CourseProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalCourseAudioStorage implements CourseAudioStorage {
    private final Path root;

    public LocalCourseAudioStorage(CourseProperties properties) {
        this.root = Path.of(properties.storageDir()).toAbsolutePath().normalize();
    }

    @Override
    public Path store(long userId, long courseId, int partNumber, MultipartFile file) {
        // 文件名完全由服务端生成，不使用客户端文件名，避免目录穿越。
        Path directory = root.resolve(Long.toString(userId)).resolve(Long.toString(courseId)).normalize();
        Path target = directory.resolve(partNumber + ".webm");
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException exception) {
            throw new CourseStorageException(exception);
        }
    }

    @Override
    public byte[] read(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new CourseStorageException(new IOException("Audio path is outside storage root"));
        }
        try {
            return Files.readAllBytes(normalized);
        } catch (IOException exception) {
            throw new CourseStorageException(exception);
        }
    }

    @Override
    public void deleteAll(List<Path> paths) {
        try {
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            throw new CourseStorageException(exception);
        }
    }
}
