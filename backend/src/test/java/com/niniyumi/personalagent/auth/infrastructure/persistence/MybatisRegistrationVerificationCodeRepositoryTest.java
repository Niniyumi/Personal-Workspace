package com.niniyumi.personalagent.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.niniyumi.personalagent.auth.domain.RegistrationVerificationCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MybatisRegistrationVerificationCodeRepositoryTest {
    @Mock RegistrationVerificationCodeMapper mapper;

    @Test
    void replaceWritesAllCodeFields() {
        RegistrationVerificationCode code = new RegistrationVerificationCode(
                "nini@example.com", "123456", Instant.parse("2026-09-02T08:10:00Z"),
                Instant.parse("2026-09-02T08:00:00Z"));
        MybatisRegistrationVerificationCodeRepository repository =
                new MybatisRegistrationVerificationCodeRepository(mapper);

        repository.replace(code);

        verify(mapper).replace("nini@example.com", "123456",
                Instant.parse("2026-09-02T08:10:00Z"), Instant.parse("2026-09-02T08:00:00Z"));
    }

    @Test
    void consumeReportsWhetherOneValidCodeWasDeleted() {
        Instant now = Instant.parse("2026-09-02T08:05:00Z");
        when(mapper.consume("nini@example.com", "123456", now)).thenReturn(1);
        MybatisRegistrationVerificationCodeRepository repository =
                new MybatisRegistrationVerificationCodeRepository(mapper);

        assertThat(repository.consume("nini@example.com", "123456", now)).isTrue();
    }
}
