package com.niniyumi.personalagent.course.infrastructure.media;

import java.util.Locale;

public enum CourseAudioFormat {
    M4A("m4a", "audio/mp4"),
    MP3("mp3", "audio/mpeg"),
    WAV("wav", "audio/wav");

    private final String extension;
    private final String mediaType;

    CourseAudioFormat(String extension, String mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() {
        return extension;
    }

    public String mediaType() {
        return mediaType;
    }

    public static CourseAudioFormat fromFileName(String fileName) {
        String normalized = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        for (CourseAudioFormat format : values()) {
            if (normalized.endsWith("." + format.extension)) return format;
        }
        throw new CourseMediaException("仅支持 M4A、MP3 或 WAV 录音文件");
    }

    public static CourseAudioFormat fromProbeName(String probeName) {
        String normalized = probeName == null ? "" : probeName.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("m4a")) return M4A;
        if (normalized.equals("mp3")) return MP3;
        if (normalized.equals("wav")) return WAV;
        throw new CourseMediaException("录音格式不受支持，请选择 M4A、MP3 或 WAV 文件");
    }
}
