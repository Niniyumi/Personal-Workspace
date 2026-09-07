package com.niniyumi.personalagent.auth.application;

public interface RegistrationVerificationMailSender {
    void send(String email, String code);
}
