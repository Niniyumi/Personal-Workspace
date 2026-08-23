package com.niniyumi.personalagent.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.niniyumi.personalagent.auth.domain.User;
import com.niniyumi.personalagent.auth.domain.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisUserRepository implements UserRepository {
    private final UserMapper userMapper;

    public MybatisUserRepository(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.selectCount(new LambdaQueryWrapper<UserRow>()
                .eq(UserRow::getUsername, username)) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<UserRow>()
                .eq(UserRow::getEmail, email)) > 0;
    }

    @Override
    public User save(User user) {
        UserRow row = UserRow.fromDomain(user);
        userMapper.insert(row);
        return row.toDomain();
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String login) {
        return Optional.ofNullable(userMapper.selectOne(new LambdaQueryWrapper<UserRow>()
                .eq(UserRow::getUsername, login)
                .or()
                .eq(UserRow::getEmail, login)))
                .map(UserRow::toDomain);
    }

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(UserRow::toDomain);
    }
}
