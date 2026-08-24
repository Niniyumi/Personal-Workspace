package com.niniyumi.personalagent.course.infrastructure.speech;

import java.nio.file.Path;

@FunctionalInterface
public interface SpeechProvider {
    String transcribe(Path audioFile);
}
