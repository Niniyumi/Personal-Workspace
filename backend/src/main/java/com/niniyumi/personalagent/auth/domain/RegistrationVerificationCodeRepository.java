package com.niniyumi.personalagent.auth.domain;

import java.time.Instant;

public interface RegistrationVerificationCodeRepository {
    void replace(RegistrationVerificationCode code);

    boolean consume(String email, String code, Instant now);
}
