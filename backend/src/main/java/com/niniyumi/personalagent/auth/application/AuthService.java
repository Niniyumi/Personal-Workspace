package com.niniyumi.personalagent.auth.application;

import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import com.niniyumi.personalagent.auth.domain.RefreshSession;
import com.niniyumi.personalagent.auth.infrastructure.security.JwtTokenService;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final String USERNAME_CONSTRAINT = "uk_users_username";
    private static final String EMAIL_CONSTRAINT = "uk_users_email";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenService jwtTokenService;
    private final RegistrationVerificationService registrationVerificationService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService, JwtTokenService jwtTokenService,
                       RegistrationVerificationService registrationVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenService = jwtTokenService;
        this.registrationVerificationService = registrationVerificationService;
    }

    @Transactional
    public User register(RegisterCommand command) {
        // 统一规范化登录标识，避免仅因大小写或首尾空格不同而创建重复账户。
        String username = command.username().trim().toLowerCase(Locale.ROOT);
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        // 应用层预检查用于快速反馈，数据库唯一索引负责并发注册时的最终兜底。
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        registrationVerificationService.consume(email, command.code());
        try {
            return userRepository.save(new User(null, username, email,
                    passwordEncoder.encode(command.password()), command.displayName().trim(),
                    UserStatus.ACTIVE, null, null));
        } catch (DuplicateKeyException exception) {
            // 并发请求可能同时通过预检查，因此按唯一索引名称转换为稳定的业务错误。
            String constraint = duplicateConstraint(exception);
            if (USERNAME_CONSTRAINT.equals(constraint)) {
                throw new UsernameAlreadyExistsException();
            }
            if (EMAIL_CONSTRAINT.equals(constraint)) {
                throw new EmailAlreadyExistsException();
            }
            throw exception;
        }
    }

    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByUsernameOrEmail(command.login())
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .filter(candidate -> passwordEncoder.matches(command.password(), candidate.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return issueTokens(user);
    }

    @Transactional
    public LoginResult refresh(String refreshToken) {
        RefreshSession session = refreshTokenService.consume(refreshToken);
        User user = userRepository.findById(session.userId())
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .orElseThrow(InvalidRefreshTokenException::new);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.consume(refreshToken);
    }

    private LoginResult issueTokens(User user) {
        return new LoginResult(jwtTokenService.issueAccessToken(user), refreshTokenService.issue(user.id()).value());
    }

    private String duplicateConstraint(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null) {
                continue;
            }
            if (message.contains(USERNAME_CONSTRAINT)) {
                return USERNAME_CONSTRAINT;
            }
            if (message.contains(EMAIL_CONSTRAINT)) {
                return EMAIL_CONSTRAINT;
            }
        }
        return null;
    }
}
