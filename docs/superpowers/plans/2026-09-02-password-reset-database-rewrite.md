# 忘记密码数据库轻量整改计划

## 结论

当前不需要重构或重写整个数据库。`users`、`refresh_sessions`、`password_reset_codes` 的拆分合理，足以支撑小规模使用。当前应保留现有表和数据，只做低风险的代码级正确性整改；开发环境如需清场，只删除一次性验证码记录。

## 当前必须做的最小整改

### 1. 原子消费验证码

保持 `password_reset_codes` 表不变，把 `markUsed` 改成条件更新：只有 `used_at IS NULL` 且 `expires_at > 当前时间` 时才能写入 `used_at`，并要求受影响行数等于 1。这样无需新表或新字段，就能避免同一验证码被重复消费。

查询最新验证码时按 `created_at DESC, id DESC` 排序，避免同一毫秒创建时结果不确定。

### 2. 密码重置后撤销刷新会话

保持 `refresh_sessions` 表不变，新增按 `user_id` 将所有未撤销、未过期会话设置 `revoked_at` 的仓储方法。密码更新、验证码消费、会话撤销放在同一个数据库事务中。

现有访问令牌最长 15 分钟后自然失效；本阶段不增加访问令牌黑名单。

### 3. 保持当前简单发信流程

继续使用同步 QQ SMTP，不引入消息队列或 outbox。发送失败时由当前事务回滚新验证码即可。对于极少发生的“邮件已发送但事务提交失败”，用户重新申请验证码即可。

## 数据处理计划

### 上线前备份

只备份认证相关三张表：

```powershell
mysqldump --single-transaction --routines=false --triggers=false work users refresh_sessions password_reset_codes > auth-before-password-reset.sql
```

备份文件应放在数据库备份目录，不提交到 Git。先在测试库执行一次恢复，确认备份可用。

### 生产/已有环境

不改写 `users`，不重新计算 `password_hash`，不改用户 ID，也不触碰周报、课程等业务表。

部署前记录基线：

```sql
SELECT COUNT(*) AS users_count FROM users;
SELECT COUNT(*) AS active_sessions
FROM refresh_sessions
WHERE revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP(3);
SELECT COUNT(*) AS reset_codes_count FROM password_reset_codes;
```

部署代码后，已有验证码仍兼容，无需转换。建议让部署前签发的验证码自然过期；如希望行为最简单，可在维护窗口执行：

```sql
DELETE FROM password_reset_codes;
```

该删除只会让旧验证码失效，用户可重新申请，不影响账户或业务数据。

### 全新开发环境

优先让 Flyway 从 V1 到 V5 自动建库，不手工创建表。若本地测试数据不需要保留，可只清空一次性认证数据：

```sql
START TRANSACTION;
DELETE FROM password_reset_codes;
DELETE FROM refresh_sessions;
COMMIT;
```

不要删除 `users`，除非明确决定丢弃全部本地账户；外键关联的业务数据使整库清空没有必要且风险更高。

## 验证清单

部署前后分别确认：

```sql
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM password_reset_codes WHERE used_at IS NULL AND expires_at > CURRENT_TIMESTAMP(3);
SELECT user_id, COUNT(*) AS active_count
FROM refresh_sessions
WHERE revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP(3)
GROUP BY user_id;
```

功能只验证五条主路径：

1. 已注册邮箱收到验证码并成功修改密码。
2. 新密码可以登录，旧密码不能登录。
3. 同一验证码第二次使用失败。
4. 重置前的刷新令牌不能再换取访问令牌。
5. 未注册邮箱申请接口仍返回与已注册邮箱相同的 204 响应。

## 回滚

- 代码回滚：恢复上一版本应用；现有表结构没有变化，无需回滚 Flyway。
- 数据回滚：本计划默认不改写用户数据，因此通常不需要恢复数据库。
- 如果上线时清空了 `password_reset_codes`，不建议恢复这些短期验证码；让用户重新申请即可。
- 只有发现 `users` 或 `refresh_sessions` 被误改时，才从上线前备份恢复对应记录，并先停止应用写入。

## 暂不实施

以下内容在当前用户规模下收益不足，先不做：Redis、验证码发送队列、outbox、验证码尝试次数表、IP/设备审计表、访问令牌黑名单、分库分表、全量用户数据重写。

触发条件：出现验证码爆破/滥发、邮件失败率明显、并发用户显著增长或合规审计要求时，再单独设计增强方案。
