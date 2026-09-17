package com.niniyumi.personalagent.course.infrastructure.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CourseAudioFormatTest {
    @Test
    void recognizesOnlyTheThreeSupportedFilenameExtensions() {
        assertThat(CourseAudioFormat.fromFileName("lesson.M4A").extension()).isEqualTo("m4a");
        assertThat(CourseAudioFormat.fromFileName("lesson.mp3").mediaType()).isEqualTo("audio/mpeg");
        assertThat(CourseAudioFormat.fromFileName("lesson.wav").mediaType()).isEqualTo("audio/wav");
        assertThatThrownBy(() -> CourseAudioFormat.fromFileName("lesson.aac"))
                .isInstanceOf(CourseMediaException.class);
    }

    @Test
    void recognizesFfprobeContainerNamesForSupportedRecordings() {
        assertThat(CourseAudioFormat.fromProbeName("mov,mp4,m4a,3gp,3g2,mj2"))
                .isEqualTo(CourseAudioFormat.M4A);
        assertThat(CourseAudioFormat.fromProbeName("mp3\n")).isEqualTo(CourseAudioFormat.MP3);
        assertThat(CourseAudioFormat.fromProbeName("wav")).isEqualTo(CourseAudioFormat.WAV);
        assertThatThrownBy(() -> CourseAudioFormat.fromProbeName("ogg"))
                .isInstanceOf(CourseMediaException.class);
    }
}
