package com.niniyumi.personalagent.auth.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.niniyumi.personalagent.auth.domain.RegistrationVerificationCode;
import com.niniyumi.personalagent.auth.domain.RegistrationVerificationCodeRepository;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationVerificationServiceTest {
    @Mock UserRepository users;
    @Mock RegistrationVerificationCodeRepository codes;
    @Mock RegistrationVerificationMailSender mailSender;
    @Mock VerificationCodeGenerator codeGenerator;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-02T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void requestReplacesTheCodeAndSendsItToTheNormalizedEmail() {
        when(users.existsByEmail("nini@example.com")).thenReturn(false);
        when(codeGenerator.generate()).thenReturn("123456");
        RegistrationVerificationService service = service();

        service.request(" NINI@example.com ");

        ArgumentCaptor<RegistrationVerificationCode> saved =
                ArgumentCaptor.forClass(RegistrationVerificationCode.class);
        verify(codes).replace(saved.capture());
        org.assertj.core.api.Assertions.assertThat(saved.getValue()).isEqualTo(
                new RegistrationVerificationCode("nini@example.com", "123456",
                        clock.instant().plusSeconds(600), clock.instant()));
        verify(mailSender).send("nini@example.com", "123456");
    }

    @Test
    void requestRejectsAnEmailThatAlreadyHasAnAccount() {
        when(users.existsByEmail("nini@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service().request("nini@example.com"))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verifyNoInteractions(codes, mailSender, codeGenerator);
    }

    @Test
    void consumeRejectsAnInvalidOrExpiredCode() {
        when(codes.consume("nini@example.com", "654321", clock.instant())).thenReturn(false);

        assertThatThrownBy(() -> service().consume("NINI@example.com", "654321"))
                .isInstanceOf(InvalidRegistrationVerificationCodeException.class);
    }

    private RegistrationVerificationService service() {
        return new RegistrationVerificationService(users, codes, mailSender, codeGenerator, clock);
    }
}
