package com.niniyumi.personalagent.auth.infrastructure.persistence;

import com.niniyumi.personalagent.auth.domain.RegistrationVerificationCode;
import com.niniyumi.personalagent.auth.domain.RegistrationVerificationCodeRepository;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisRegistrationVerificationCodeRepository
        implements RegistrationVerificationCodeRepository {
    private final RegistrationVerificationCodeMapper mapper;

    public MybatisRegistrationVerificationCodeRepository(RegistrationVerificationCodeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void replace(RegistrationVerificationCode code) {
        mapper.replace(code.email(), code.code(), code.expiresAt(), code.createdAt());
    }

    @Override
    public boolean consume(String email, String code, Instant now) {
        return mapper.consume(email, code, now) == 1;
    }
}
