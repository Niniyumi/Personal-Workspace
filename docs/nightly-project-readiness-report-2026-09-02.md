# 夜间项目检查报告（2026-09-02）

> 2026-09-07 更新：为兼容 IDEA 2021.3，项目运行基线已从 Java 21 调整为 Java 17；下方 Java 21 内容是 2026-09-02 当晚检查时的历史状态。

## 2026-09-07 补充进展

- 已将项目的 Maven 编译版本、IDEA 项目语言级别、模块语言级别和字节码目标统一为 Java 17。
- 已把两处 Java 21 专用的 `List.getFirst()` 改为 Java 17 可用的 `List.get(0)`，没有改变原有业务流程。
- 本机已安装 Eclipse Temurin JDK 17.0.20.1，路径为 `C:\Users\18934\AppData\Local\Programs\Eclipse Adoptium\jdk-17`；原有 JDK 22 保留。
- IDEA 中最初显示 `17 (invalid)` 的原因是项目引用了名为 `17` 的 SDK，但 IDEA 全局 SDK 表尚未登记对应目录；现已完成登记。
- 后续发现 Maven 编译插件仍残留 `source=16`、`target=16`，IDEA 因此把模块字节码目标重新写成 16，导致 `PasswordResetService` 无法识别 Java 17 的 `HexFormat`。现已改为使用统一的 `<release>${java.version}</release>`，IDEA 模块目标也恢复为 17。
- 使用真实 JDK 17 离线执行清理编译，134 个后端源码文件均以 `release 17` 编译，结果为 `BUILD SUCCESS`。
- 远程提交前再次集中验证：后端测试 127/127、前端测试 75/75 全部通过，TypeScript 检查和 Vite 生产构建成功；验证过程中未连接真实 SMTP，也未调用阿里模型接口。

当前最短下一步：在 IDEA 的 Maven 面板执行一次 `Reload All Maven Projects`，随后 `Build → Rebuild Project`；Tomcat 运行配置中的 JRE 选择已安装的 JDK 17。IDEA 编译跑通后，再配置数据库、JWT 和邮箱环境变量，完成一次注册验证码、注册、忘记密码和重设密码的真实邮箱主流程冒烟测试。

## 结论

项目核心第一阶段已经成形：认证、周报、工作总结、课程录音转笔记都有后端接口、前端页面和自动化测试。注册邮箱验证码已按最小方案接入；不需要重写认证数据库，只需新增 `registration_verification_codes` 表的 V6 增量迁移。

目前不能直接在本机完成真实启动和发信，原因不是代码依赖，而是本机环境变量尚未配置。今晚没有修改真实数据库，也没有发送真实邮件。

## IDEA 与本机部署

- 实际安装的是 IntelliJ IDEA Ultimate 2021.2（build `212.4746.92`），没有发现 2022.3。
- 项目 Maven 目标是 Java 21；现有 `.idea` 元数据却同时出现语言级别 16、字节码 21、SDK 名称 22，需在新 IDE 中统一。
- 本机已有 Java 22.0.2、Node 22.22.0；MySQL80 服务正在运行。
- Spring Boot 3.5.16 官方要求 Java 17 以上并兼容 Java 25，因此 Java 22 本身可运行项目。[Spring Boot 3.5 系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- JetBrains 官方版本表显示 IDEA 2022.3 的 Java 支持到 19，而 Java 21 完整支持从 IDEA 2023.3 开始。[JetBrains Java 支持版本表](https://www.jetbrains.com/help/idea/supported-java-versions.html)

建议不要为了迁就 IDEA 2022.3 把项目降级到旧 Java。最省事的选择是：安装 IDEA 2024.1 或更高，并继续使用本机 JDK 22；如果使用 IDEA 2023.3，则另配 JDK 21。

### 明天你要做的最短步骤

1. 决定使用 IDEA 2024.1+（推荐）还是 IDEA 2023.3 + JDK 21。当前 2021.2 不建议继续配置本项目。
2. 在 IDEA 打开项目根目录，等待 Maven 导入 `backend/pom.xml`。
3. 将 Project SDK 和 Maven Runner JRE 设为同一个 JDK；Language level 设为 `21`。
4. 在 Spring Boot 运行配置中填写 `DB_PASSWORD`、`JWT_SECRET`、`MAIL_USERNAME`、`MAIL_AUTH_CODE`。不要写入 Git。
5. 开发时可直接运行后端主类 `PersonalAgentApplication`；需要部署时按 [IDEA + 可执行 JAR](development/idea-jar.md) 构建。

## 邮箱流程验证

自动化覆盖的主路径：

1. 请求注册验证码，旧验证码被同邮箱新请求替换。
2. 提交邮箱、验证码、用户名和密码完成注册；错误或过期验证码返回明确的 400。
3. 请求找回密码验证码；未知邮箱仍返回统一响应。
4. 使用有效验证码重设密码，验证码被条件消费，并撤销该用户已有刷新会话。
5. 前端注册页能够发送验证码并把验证码随注册请求提交；找回密码页面流程保持可用。
6. 打包部署后直接访问 `/forgot-password` 会正确转发到 Vue 入口。

真实 QQ SMTP 投递暂未验证，因为 `MAIL_USERNAME` 和 `MAIL_AUTH_CODE` 未设置，也没有指定测试收件地址。明天只需做一次真实邮箱冒烟，不需要扩大测试规模。

验证结果：邮箱相关后端定向测试 40/40、前端定向测试 22/22；最终后端全量测试 127/127、前端全量测试 75/75；TypeScript 检查与 Vite 生产构建成功，`backend/target/personal-agent.war` 打包成功。测试均使用本地已有依赖，未连接真实 SMTP。

## 数据库结论

不建议整库重构。V6 只新增一张很小的注册验证码表，现有 `users`、`password_reset_codes`、`refresh_sessions` 继续使用。详细的低风险迁移、备份、回滚与数据检查步骤见 [密码重置数据库整改计划](superpowers/plans/2026-09-02-password-reset-database-rewrite.md)。

注册验证码按用户要求短期明文保存，密码仍使用 BCrypt。等实际用户量、攻击风险或审计要求上升后，再考虑验证码摘要、发送频率限制和清理任务；现在不引入 Redis、队列或复杂风控。

## 核心功能状态

- 认证：注册、注册邮箱验证、登录、刷新、退出、找回密码均已实现，主路径接近可用；差真实 SMTP + MySQL 冒烟。
- 周报：创建、列表、详情、编辑、DOCX 导入已实现；无 AI Key 时仍可手工编辑。
- 工作总结：生成、查询、编辑已实现；AI 生成需要模型 Key。
- 课程：创建、分片上传、完成处理、重试、编辑与导出笔记已实现；真实语音转写需要 DashScope Key。
- 工程化：后端 WAR 能打包前端产物，Flyway 有 V1–V6；IDE 配置和本机密钥是当前主要部署缺口。

## 明日只需决定三件事

1. IDEA 版本：建议 2024.1+；不要选 2022.3。
2. 邮件发件账号与测试收件邮箱，并提供 QQ 邮箱 SMTP 授权码到本机环境变量。
3. 邮件措辞选 A/B/C；建议直接选 [方案 A 极简版](email-verification-copy-options.md)。
