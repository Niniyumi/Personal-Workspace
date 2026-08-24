# IDEA + Tomcat 本地部署

本项目开发时仍推荐分别运行 Vite 和 Spring Boot。需要按传统 Web 项目演示时，再使用本页的 WAR 部署方式。当前项目要求 Java 21 与 Tomcat 10.1。

## 1. 构建前端和 WAR

在项目根目录依次执行：

```powershell
Set-Location frontend
npm run build
Set-Location ../backend
.\mvnw.cmd clean package
```

成功后会生成 `backend/target/personal-agent.war`。WAR 已包含 Vue 的 `index.html` 和 `assets`，但不会包含 `application-local.yml`、`application-secret.yml` 或其他本机密钥。

## 2. 在 IDEA 中添加 Tomcat

该功能需要 IntelliJ IDEA Ultimate。打开 `Run | Edit Configurations`：

1. 点击 `+`，选择 `Tomcat Server | Local`。
2. `Application server` 选择 Tomcat 10.1 安装目录，例如 `D:\software\apache-tomcat-10.1.56`。
3. `JRE` 选择 Java 21。
4. 在 `Deployment` 页点击 `+`，选择 `Artifact`，添加 `personal-agent:war exploded`；如果 IDEA 尚未识别 Artifact，可先执行 Maven `package`，再重新导入 `backend/pom.xml`。
5. 将 `Application context` 设置为 `/`，这样网页地址是 `http://localhost:8080/`。
6. `On Update action` 和 `On frame deactivation` 都选择 `Update classes and resources`，便于调试期间更新静态资源和 Java 类。

## 3. 配置本机环境

在 Tomcat 运行配置的 `Environment variables` 中填写：

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/work?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=<你的本机 MySQL 密码>
JWT_SECRET=<至少 32 字节的随机密钥>
```

不要把真实密码或 JWT 密钥写进 Git 文件。可以在 PowerShell 中生成 JWT 密钥：

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

## 4. 启动与检查

启动 Tomcat 后检查：

- `http://localhost:8080/login` 显示 Vue 登录页。
- 登录请求进入 `/api/auth/login`，而不是被前端路由接管。
- 直接打开 `/weekly-reports/42` 返回 Vue 页面，不出现 Tomcat 404。
- 未带令牌访问 `/api/users/me` 返回 `401` JSON。

如果 8080 已被 Spring Boot 或其他 Tomcat 占用，只保留一个服务，或在 Tomcat `Server` 页修改 HTTP port。修改端口后，浏览器地址也要使用新端口。

每次前端代码变化后先重新执行 `npm run build`，再在 IDEA 中使用 `Update` 或重新部署；Maven 不会替你运行 npm，这能避免构建链变慢和重复安装 Node 依赖。

## 已验证环境

2026-08-24 已使用 Java 21、Tomcat 10.1.56、MySQL 8 和根上下文 `/` 完成外部 Tomcat 冒烟测试：登录页和周报详情前端路由返回 200，未授权 API 返回 401，错误凭据进入认证接口并返回 `INVALID_CREDENTIALS`。
