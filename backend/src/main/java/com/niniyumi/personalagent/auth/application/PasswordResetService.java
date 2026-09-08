package com.niniyumi.personalagent.auth.application;

import com.niniyumi.personalagent.auth.domain.PasswordResetCode;
import com.niniyumi.personalagent.auth.domain.PasswordResetCodeRepository;
import com.niniyumi.personalagent.auth.domain.RefreshSessionRepository;
import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private final UserRepository users;
    private final PasswordResetCodeRepository codes;
    private final RefreshSessionRepository refreshSessions;
    private final PasswordResetMailSender mailSender;
    private final VerificationCodeGenerator codeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PasswordResetService(UserRepository users, PasswordResetCodeRepository codes,
            RefreshSessionRepository refreshSessions,
            PasswordResetMailSender mailSender, VerificationCodeGenerator codeGenerator,
            PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.codes = codes;
        this.refreshSessions = refreshSessions;
        this.mailSender = mailSender;
        this.codeGenerator = codeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public void request(String rawEmail) {
        String email = normalize(rawEmail);
        User user = users.findByUsernameOrEmail(email).orElse(null);
        if (user == null) {
            log.info("Password reset email skipped because no user matched, recipient={}", mask(email));
            return;
        }
        codes.deleteAllByUserId(user.id());
        String code = codeGenerator.generate();
        Instant now = clock.instant();
        codes.save(new PasswordResetCode(null, user.id(), hash(code), now.plusSeconds(600), null, now));
        mailSender.send(email, code);
    }

    @Transactional
    public void confirm(String rawEmail, String code, String newPassword) {
        User user = users.findByUsernameOrEmail(normalize(rawEmail))
                .orElseThrow(InvalidPasswordResetCodeException::new);
        Instant now = clock.instant();
        PasswordResetCode latest = codes.findLatestByUserId(user.id())
                .filter(saved -> saved.isValidAt(now))
                .filter(saved -> MessageDigest.isEqual(
                        saved.codeHash().getBytes(StandardCharsets.UTF_8),
                        hash(code).getBytes(StandardCharsets.UTF_8)))
                .orElseThrow(InvalidPasswordResetCodeException::new);
        if (!codes.markUsedIfValid(latest.id(), now)) {
            throw new InvalidPasswordResetCodeException();
        }
        users.updatePassword(user.id(), passwordEncoder.encode(newPassword));
        refreshSessions.revokeAllActiveByUserId(user.id(), now);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
