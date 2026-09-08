package com.niniyumi.personalagent.auth.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.niniyumi.personalagent.auth.domain.PasswordResetCode;
import com.niniyumi.personalagent.auth.domain.PasswordResetCodeRepository;
import com.niniyumi.personalagent.auth.domain.RefreshSessionRepository;
import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class PasswordResetServiceTest {
    @Mock UserRepository users;
    @Mock PasswordResetCodeRepository codes;
    @Mock PasswordResetMailSender mailSender;
    @Mock VerificationCodeGenerator codeGenerator;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RefreshSessionRepository refreshSessions;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-25T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void issuedCodeResetsThePasswordOnce() {
        User user = new User(42L, "nini", "nini@example.com", "old-hash", "Nini",
                UserStatus.ACTIVE, null, null);
        when(users.findByUsernameOrEmail("nini@example.com")).thenReturn(Optional.of(user));
        when(codeGenerator.generate()).thenReturn("123456");
        when(passwordEncoder.encode("NewPassword8!")).thenReturn("new-hash");
        PasswordResetService service = new PasswordResetService(
                users, codes, refreshSessions, mailSender, codeGenerator, passwordEncoder, clock);

        service.request("NINI@example.com");
        ArgumentCaptor<PasswordResetCode> saved = ArgumentCaptor.forClass(PasswordResetCode.class);
        verify(codes).save(saved.capture());
        verify(mailSender).send("nini@example.com", "123456");
        PasswordResetCode latest = new PasswordResetCode(7L, 42L, saved.getValue().codeHash(),
                saved.getValue().expiresAt(), null, saved.getValue().createdAt());
        when(codes.findLatestByUserId(42L)).thenReturn(Optional.of(latest));
        when(codes.markUsedIfValid(7L, clock.instant())).thenReturn(true);

        service.confirm("nini@example.com", "123456", "NewPassword8!");

        verify(users).updatePassword(42L, "new-hash");
        verify(codes).markUsedIfValid(7L, clock.instant());
        verify(refreshSessions).revokeAllActiveByUserId(42L, clock.instant());
    }

    @Test
    void unknownEmailReturnsNormallyWithoutSendingMail(CapturedOutput output) {
        when(users.findByUsernameOrEmail("missing@example.com")).thenReturn(Optional.empty());
        PasswordResetService service = new PasswordResetService(
                users, codes, refreshSessions, mailSender, codeGenerator, passwordEncoder, clock);

        service.request("missing@example.com");

        verifyNoInteractions(codes, mailSender, codeGenerator, passwordEncoder);
        org.assertj.core.api.Assertions.assertThat(output)
                .contains("Password reset email skipped")
                .contains("mi***@example.com")
                .doesNotContain("missing@example.com");
    }

    @Test
    void wrongCodeDoesNotChangePassword() {
        User user = new User(42L, "nini", "nini@example.com", "old-hash", "Nini",
                UserStatus.ACTIVE, null, null);
        when(users.findByUsernameOrEmail("nini@example.com")).thenReturn(Optional.of(user));
        when(codes.findLatestByUserId(42L)).thenReturn(Optional.of(
                new PasswordResetCode(7L, 42L, "invalid-hash", clock.instant().plusSeconds(600), null, clock.instant())));
        PasswordResetService service = new PasswordResetService(
                users, codes, refreshSessions, mailSender, codeGenerator, passwordEncoder, clock);

        assertThatThrownBy(() -> service.confirm("nini@example.com", "123456", "NewPassword8!"))
                .isInstanceOf(InvalidPasswordResetCodeException.class);
    }

    @Test
    void resetFailsWhenTheCodeCannotBeConsumedAtomically() {
        User user = new User(42L, "nini", "nini@example.com", "old-hash", "Nini",
                UserStatus.ACTIVE, null, null);
        PasswordResetCode saved = new PasswordResetCode(7L, 42L,
                "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92",
                clock.instant().plusSeconds(600), null, clock.instant());
        when(users.findByUsernameOrEmail("nini@example.com")).thenReturn(Optional.of(user));
        when(codes.findLatestByUserId(42L)).thenReturn(Optional.of(saved));
        when(codes.markUsedIfValid(7L, clock.instant())).thenReturn(false);
        PasswordResetService service = new PasswordResetService(
                users, codes, refreshSessions, mailSender, codeGenerator, passwordEncoder, clock);

        assertThatThrownBy(() -> service.confirm("nini@example.com", "123456", "NewPassword8!"))
                .isInstanceOf(InvalidPasswordResetCodeException.class);

        verifyNoInteractions(passwordEncoder, refreshSessions);
    }
}
