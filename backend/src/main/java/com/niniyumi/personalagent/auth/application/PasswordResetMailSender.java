package com.niniyumi.personalagent.auth.application;

public interface PasswordResetMailSender {
    void send(String email, String code);
}
