package com.niniyumi.personalagent.course.infrastructure.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FfmpegCourseAudioSegmenter implements CourseAudioSegmenter, CourseAudioInspector {
    private static final long MAX_SEGMENT_BYTES = 10L * 1024 * 1024;
    private final String ffmpeg;
    private final String ffprobe;

    public FfmpegCourseAudioSegmenter(
            @Value("${app.course.ffmpeg-path:ffmpeg}") String ffmpeg,
            @Value("${app.course.ffprobe-path:ffprobe}") String ffprobe) {
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;
    }

    @Override
    public List<Segment> split(Path original, Path outputDirectory) {
        double fullDuration = probe(original);
        if (fullDuration < 1 || fullDuration > 9000) {
            throw new CourseMediaException("录音时长须在 1 秒到 150 分钟之间");
        }
        run(List.of(ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-i", original.toString(),
                "-map", "0:a:0", "-vn", "-ac", "1", "-c:a", "libopus", "-b:a", "32k",
                "-f", "segment", "-segment_time", "270", "-reset_timestamps", "1",
                outputDirectory.resolve("part-%03d.webm").toString()));
        try (var files = Files.list(outputDirectory)) {
            List<Path> paths = files.filter(path -> path.getFileName().toString().matches("part-\\d{3}\\.webm"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
            if (paths.isEmpty()) throw new CourseMediaException("录音文件没有可识别的音轨");
            List<Segment> segments = new ArrayList<>();
            for (Path path : paths) {
                long bytes = Files.size(path);
                double duration = probe(path);
                if (bytes == 0 || bytes > MAX_SEGMENT_BYTES || duration < 1 || duration > 300) {
                    throw new CourseMediaException("录音切段超过语音识别限制");
                }
                segments.add(new Segment(path, (int) Math.ceil(duration), bytes));
            }
            return segments;
        } catch (IOException exception) {
            throw new CourseMediaException("录音切段读取失败", exception);
        }
    }

    @Override
    public CourseAudioFormat inspect(Path original) {
        String format = run(List.of(ffprobe, "-v", "error", "-show_entries", "format=format_name",
                "-of", "default=noprint_wrappers=1:nokey=1", original.toString()));
        CourseAudioFormat detectedFormat = CourseAudioFormat.fromProbeName(format);
        String streamType = run(List.of(ffprobe, "-v", "error", "-select_streams", "a:0",
                "-show_entries", "stream=codec_type",
                "-of", "default=noprint_wrappers=1:nokey=1", original.toString()));
        if (!"audio".equals(streamType.trim())) throw new CourseMediaException("录音文件没有可识别的音轨");
        double duration = probe(original);
        if (duration < 1 || duration > 9000) {
            throw new CourseMediaException("录音时长须在 1 秒到 150 分钟之间");
        }
        return detectedFormat;
    }

    private double probe(Path path) {
        String output = run(List.of(ffprobe, "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", path.toString()));
        try {
            return Double.parseDouble(output.trim());
        } catch (NumberFormatException exception) {
            throw new CourseMediaException("无法识别录音时长", exception);
        }
    }

    private String run(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readNBytes(8192), java.nio.charset.StandardCharsets.UTF_8);
            if (process.waitFor() != 0) throw new CourseMediaException("FFmpeg 无法处理此录音，请确认文件格式正确");
            return output;
        } catch (IOException exception) {
            throw new CourseMediaException("FFmpeg/ffprobe 未安装或路径配置错误", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CourseMediaException("录音处理被中断", exception);
        }
    }
}
