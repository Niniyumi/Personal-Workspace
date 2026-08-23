package com.niniyumi.personalagent.auth.application;

import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import com.niniyumi.personalagent.auth.domain.RefreshSession;
import com.niniyumi.personalagent.auth.infrastructure.security.JwtTokenService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public User register(RegisterCommand command) {
        String username = command.username().trim().toLowerCase(Locale.ROOT);
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException();
        }
        if (userRepository.findByUsernameOrEmail(username).isPresent()) {
            throw new UsernameAlreadyExistsException();
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        if (userRepository.findByUsernameOrEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException();
        }
        try {
            return userRepository.save(new User(null, username, email,
                    passwordEncoder.encode(command.password()), command.displayName().trim(),
                    UserStatus.ACTIVE, null, null));
        } catch (DuplicateKeyException exception) {
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
        List<Throwable> causes = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = exception; current != null && seen.add(current); current = current.getCause()) {
            causes.add(current);
        }
        for (int index = causes.size() - 1; index >= 0; index--) {
            String message = causes.get(index).getMessage();
            if (message == null) {
                continue;
            }
            int usernameIndex = message.lastIndexOf(USERNAME_CONSTRAINT);
            int emailIndex = message.lastIndexOf(EMAIL_CONSTRAINT);
            if (usernameIndex >= 0 || emailIndex >= 0) {
                return usernameIndex > emailIndex ? USERNAME_CONSTRAINT : EMAIL_CONSTRAINT;
            }
        }
        return null;
    }
}
