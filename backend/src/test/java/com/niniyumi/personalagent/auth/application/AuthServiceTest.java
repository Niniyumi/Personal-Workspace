package com.niniyumi.personalagent.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.RefreshSession;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import com.niniyumi.personalagent.auth.infrastructure.security.JwtTokenService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RegistrationVerificationService registrationVerificationService;

    private AuthService service;
    private RegisterCommand command;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, refreshTokenService, jwtTokenService,
                registrationVerificationService);
        command = new RegisterCommand("nini", "nini@example.com", "UnitTest7!", "Nini", "123456");
    }

    @Test
    void registerHashesPasswordAndSavesActiveUser() {
        when(userRepository.existsByUsername("nini")).thenReturn(false);
        when(userRepository.existsByEmail("nini@example.com")).thenReturn(false);
        when(passwordEncoder.encode("UnitTest7!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), 42L));

        User user = service.register(command);

        assertThat(user.id()).isEqualTo(42L);
        assertThat(user.passwordHash()).isEqualTo("bcrypt-hash");
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).existsByUsername("nini");
        verify(userRepository).existsByEmail("nini@example.com");
        verify(userRepository).save(any());
        verify(registrationVerificationService).consume("nini@example.com", "123456");
        verifyNoMoreInteractions(userRepository);
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

    @Test
    void registerTranslatesUsernameConstraintFromMostSpecificCause() {
        when(userRepository.existsByUsername("nini")).thenReturn(false);
        when(userRepository.existsByEmail("nini@example.com")).thenReturn(false);
        when(passwordEncoder.encode("UnitTest7!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any())).thenThrow(new DuplicateKeyException("write failed",
                new IllegalStateException("Duplicate entry for key 'work.users.uk_users_username'")));

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void registerTranslatesEmailConstraintFromOuterExceptionMessage() {
        when(userRepository.existsByUsername("nini")).thenReturn(false);
        when(userRepository.existsByEmail("nini@example.com")).thenReturn(false);
        when(passwordEncoder.encode("UnitTest7!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any())).thenThrow(new DuplicateKeyException(
                "Duplicate entry for key 'work.users.uk_users_email'",
                new IllegalStateException("driver failure")));

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void registerPreservesUnidentifiedDuplicateKeyFailure() {
        DuplicateKeyException duplicateKey = new DuplicateKeyException(
                "Duplicate entry for key 'work.users.uk_refresh_sessions_token_hash'");
        when(userRepository.existsByUsername("nini")).thenReturn(false);
        when(userRepository.existsByEmail("nini@example.com")).thenReturn(false);
        when(passwordEncoder.encode("UnitTest7!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any())).thenThrow(duplicateKey);

        assertThatThrownBy(() -> service.register(command))
                .isSameAs(duplicateKey);
    }

    @Test
    void loginReturnsAccessAndRefreshTokensForActiveUserWithMatchingPassword() {
        User user = activeUser();
        when(userRepository.findByUsernameOrEmail("nini")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("UnitTest7!", "bcrypt-hash")).thenReturn(true);
        when(jwtTokenService.issueAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.issue(42L)).thenReturn(new IssuedRefreshToken("refresh-token", session(1L)));

        LoginResult result = service.login(new LoginCommand("nini", "UnitTest7!"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void loginRejectsMissingUserWithGenericCredentialsFailure() {
        when(userRepository.findByUsernameOrEmail("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand("missing", "UnitTest7!")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsWrongPasswordWithGenericCredentialsFailure() {
        when(userRepository.findByUsernameOrEmail("nini")).thenReturn(Optional.of(activeUser()));
        when(passwordEncoder.matches("UnitTest7!", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("nini", "UnitTest7!")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsDisabledUserBeforePasswordVerification() {
        User disabledUser = new User(42L, "nini", "nini@example.com", "bcrypt-hash", "Nini",
                UserStatus.DISABLED, null, null);
        when(userRepository.findByUsernameOrEmail("nini")).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> service.login(new LoginCommand("nini", "UnitTest7!")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshRevokesUsedActiveSessionBeforeIssuingReplacement() {
        User user = activeUser();
        RefreshSession usedSession = session(1L);
        when(refreshTokenService.consume("used-token")).thenReturn(usedSession);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(jwtTokenService.issueAccessToken(user)).thenReturn("new-access-token");
        when(refreshTokenService.issue(42L)).thenReturn(new IssuedRefreshToken("new-refresh-token", session(2L)));

        LoginResult result = service.refresh("used-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        InOrder order = inOrder(refreshTokenService);
        order.verify(refreshTokenService).consume("used-token");
        order.verify(refreshTokenService).issue(42L);
    }

    @Test
    void logoutRevokesMatchingActiveSession() {
        RefreshSession usedSession = session(1L);
        when(refreshTokenService.consume("used-token")).thenReturn(usedSession);

        service.logout("used-token");

        org.mockito.Mockito.verify(refreshTokenService).consume(eq("used-token"));
    }

    private User withId(User user, long id) {
        return new User(id, user.username(), user.email(), user.passwordHash(), user.displayName(),
                user.status(), user.createdAt(), user.updatedAt());
    }

    private User activeUser() {
        return new User(42L, "nini", "nini@example.com", "bcrypt-hash", "Nini",
                UserStatus.ACTIVE, null, null);
    }

    private RefreshSession session(long id) {
        return new RefreshSession(id, 42L, "hash", Instant.parse("2026-08-30T08:00:00Z"), null,
                Instant.parse("2026-08-23T08:00:00Z"));
    }
}
