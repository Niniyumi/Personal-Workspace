package com.niniyumi.personalagent.auth.api;

import com.niniyumi.personalagent.auth.api.dto.UserResponse;
import com.niniyumi.personalagent.auth.application.CurrentUserDisabledException;
import com.niniyumi.personalagent.auth.application.CurrentUserNotFoundException;
import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import com.niniyumi.personalagent.auth.domain.UserStatus;
import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/users")
public class CurrentUserController {
    private final UserRepository userRepository;

    public CurrentUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse currentUser(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(CurrentUserNotFoundException::new);
        if (user.status() != UserStatus.ACTIVE) {
            throw new CurrentUserDisabledException();
        }
        return new UserResponse(user.id(), user.username(), user.email(), user.displayName());
    }
}
