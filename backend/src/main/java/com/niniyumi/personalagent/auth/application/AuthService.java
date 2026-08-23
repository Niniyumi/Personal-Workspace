package com.niniyumi.personalagent.auth.application;

import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
