package com.niniyumi.personalagent.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService service;
    private RegisterCommand command;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder);
        command = new RegisterCommand("nini", "nini@example.com", "Password123", "Nini");
    }

    @Test
    void registerHashesPasswordAndSavesActiveUser() {
        when(userRepository.existsByUsername("nini")).thenReturn(false);
        when(userRepository.existsByEmail("nini@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 42L));

        User user = service.register(command);

        assertThat(user.id()).isEqualTo(42L);
        assertThat(user.passwordHash()).isEqualTo("bcrypt-hash");
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("nini")).thenReturn(true);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByUsername("nini")).thenReturn(false);
        when(userRepository.existsByEmail("nini@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    private User withId(User user, long id) {
        return new User(id, user.username(), user.email(), user.passwordHash(), user.displayName(),
                user.status(), user.createdAt(), user.updatedAt());
    }
}
