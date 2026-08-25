package com.niniyumi.personalagent.auth.application;

import com.niniyumi.personalagent.auth.domain.PasswordResetCode;
import com.niniyumi.personalagent.auth.domain.PasswordResetCodeRepository;
import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordResetCodeRepository codes;
    private final PasswordResetMailSender mailSender;
    private final VerificationCodeGenerator codeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PasswordResetService(UserRepository users, PasswordResetCodeRepository codes,
            PasswordResetMailSender mailSender, VerificationCodeGenerator codeGenerator,
            PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.codes = codes;
        this.mailSender = mailSender;
        this.codeGenerator = codeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public void request(String rawEmail) {
        String email = normalize(rawEmail);
        User user = users.findByUsernameOrEmail(email).orElse(null);
        if (user == null) return;
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
        PasswordResetCode latest = codes.findLatestByUserId(user.id())
                .filter(saved -> saved.isValidAt(clock.instant()))
                .filter(saved -> MessageDigest.isEqual(
                        saved.codeHash().getBytes(StandardCharsets.UTF_8),
                        hash(code).getBytes(StandardCharsets.UTF_8)))
                .orElseThrow(InvalidPasswordResetCodeException::new);
        users.updatePassword(user.id(), passwordEncoder.encode(newPassword));
        codes.markUsed(latest.id(), clock.instant());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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
