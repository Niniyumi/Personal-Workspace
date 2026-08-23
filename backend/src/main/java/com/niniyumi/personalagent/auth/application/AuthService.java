package com.niniyumi.personalagent.auth.application;

import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import com.niniyumi.personalagent.auth.domain.RefreshSession;
import com.niniyumi.personalagent.auth.infrastructure.security.JwtTokenService;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
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
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        return userRepository.save(new User(null, username, email,
                passwordEncoder.encode(command.password()), command.displayName().trim(),
                UserStatus.ACTIVE, null, null));
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
        RefreshSession session = refreshTokenService.validate(refreshToken);
        User user = userRepository.findById(session.userId())
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .orElseThrow(InvalidRefreshTokenException::new);
        refreshTokenService.revoke(session);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshTokenService.validate(refreshToken));
    }

    private LoginResult issueTokens(User user) {
        return new LoginResult(jwtTokenService.issueAccessToken(user), refreshTokenService.issue(user.id()).value());
    }
}
