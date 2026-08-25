package com.niniyumi.personalagent.auth.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.niniyumi.personalagent.auth.domain.PasswordResetCode;
import com.niniyumi.personalagent.auth.domain.PasswordResetCodeRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    @Mock UserRepository users;
    @Mock PasswordResetCodeRepository codes;
    @Mock PasswordResetMailSender mailSender;
    @Mock VerificationCodeGenerator codeGenerator;
    @Mock PasswordEncoder passwordEncoder;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-25T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void issuedCodeResetsThePasswordOnce() {
        User user = new User(42L, "nini", "nini@example.com", "old-hash", "Nini",
                UserStatus.ACTIVE, null, null);
        when(users.findByUsernameOrEmail("nini@example.com")).thenReturn(Optional.of(user));
        when(codeGenerator.generate()).thenReturn("123456");
        when(passwordEncoder.encode("NewPassword8!")).thenReturn("new-hash");
        PasswordResetService service = new PasswordResetService(
                users, codes, mailSender, codeGenerator, passwordEncoder, clock);

        service.request("NINI@example.com");
        ArgumentCaptor<PasswordResetCode> saved = ArgumentCaptor.forClass(PasswordResetCode.class);
        verify(codes).save(saved.capture());
        verify(mailSender).send("nini@example.com", "123456");
        PasswordResetCode latest = new PasswordResetCode(7L, 42L, saved.getValue().codeHash(),
                saved.getValue().expiresAt(), null, saved.getValue().createdAt());
        when(codes.findLatestByUserId(42L)).thenReturn(Optional.of(latest));

        service.confirm("nini@example.com", "123456", "NewPassword8!");

        verify(users).updatePassword(42L, "new-hash");
        verify(codes).markUsed(7L, clock.instant());
    }

    @Test
    void unknownEmailReturnsNormallyWithoutSendingMail() {
        when(users.findByUsernameOrEmail("missing@example.com")).thenReturn(Optional.empty());
        PasswordResetService service = new PasswordResetService(
                users, codes, mailSender, codeGenerator, passwordEncoder, clock);

        service.request("missing@example.com");

        verifyNoInteractions(codes, mailSender, codeGenerator, passwordEncoder);
    }

    @Test
    void wrongCodeDoesNotChangePassword() {
        User user = new User(42L, "nini", "nini@example.com", "old-hash", "Nini",
                UserStatus.ACTIVE, null, null);
        when(users.findByUsernameOrEmail("nini@example.com")).thenReturn(Optional.of(user));
        when(codes.findLatestByUserId(42L)).thenReturn(Optional.of(
                new PasswordResetCode(7L, 42L, "invalid-hash", clock.instant().plusSeconds(600), null, clock.instant())));
        PasswordResetService service = new PasswordResetService(
                users, codes, mailSender, codeGenerator, passwordEncoder, clock);

        assertThatThrownBy(() -> service.confirm("nini@example.com", "123456", "NewPassword8!"))
                .isInstanceOf(InvalidPasswordResetCodeException.class);
    }
}
