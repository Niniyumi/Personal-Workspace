package com.niniyumi.personalagent.auth.application;

import com.niniyumi.personalagent.auth.domain.RegistrationVerificationCode;
import com.niniyumi.personalagent.auth.domain.RegistrationVerificationCodeRepository;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationVerificationService {
    private final UserRepository users;
    private final RegistrationVerificationCodeRepository codes;
    private final RegistrationVerificationMailSender mailSender;
    private final VerificationCodeGenerator codeGenerator;
    private final Clock clock;

    public RegistrationVerificationService(UserRepository users,
            RegistrationVerificationCodeRepository codes,
            RegistrationVerificationMailSender mailSender,
            VerificationCodeGenerator codeGenerator, Clock clock) {
        this.users = users;
        this.codes = codes;
        this.mailSender = mailSender;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
    }

    @Transactional
    public void request(String rawEmail) {
        String email = normalize(rawEmail);
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        String code = codeGenerator.generate();
        Instant now = clock.instant();
        codes.replace(new RegistrationVerificationCode(
                email, code, now.plusSeconds(600), now));
        mailSender.send(email, code);
    }

    public void consume(String rawEmail, String code) {
        if (!codes.consume(normalize(rawEmail), code, clock.instant())) {
            throw new InvalidRegistrationVerificationCodeException();
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
