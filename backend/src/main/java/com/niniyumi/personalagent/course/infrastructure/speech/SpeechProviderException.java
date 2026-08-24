package com.niniyumi.personalagent.course.infrastructure.speech;

public class SpeechProviderException extends RuntimeException {
    public SpeechProviderException(String message) {
        super(message);
    }

    public SpeechProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
