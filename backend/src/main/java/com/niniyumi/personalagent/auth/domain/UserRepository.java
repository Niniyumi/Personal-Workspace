package com.niniyumi.personalagent.auth.domain;

import java.util.Optional;

public interface UserRepository {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    User save(User user);

    Optional<User> findByUsernameOrEmail(String login);

    Optional<User> findById(long id);
}
