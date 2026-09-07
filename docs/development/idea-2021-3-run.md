# 在 IntelliJ IDEA 2021.3 中运行项目

## 先说明当前环境

- 当前电脑实际检测到的是 IntelliJ IDEA Ultimate 2021.2（build 212.4746.92），不是 2021.3。
- 当前电脑有 Java 22.0.2 和 Node 22.22.0，但尚未检测到单独安装的 JDK 17。
- 项目 `backend/pom.xml` 的编译目标已统一为 Java 17。
- IDEA 2021.3 官方完整支持 Java 17。继续配置前，需要安装 JDK 17，并在 IDEA 中把它添加为名称为 `17` 的 SDK。

优先采用下面的“内置 Tomcat”方式。它不需要另外下载 Tomcat。

## 方式一：使用 Spring Boot 内置 Tomcat（推荐）

### 1. 打开项目

在 IDEA 中选择 `File > Open`，打开：

```text
D:\pp-program\Personal-Workspace
```

如果 IDEA 询问是否信任项目，选择信任。等待右下角 Maven 导入结束。

### 2. 配置 JDK 17

1. 打开 `File > Project Structure > Project`。
2. 在 `Project SDK` 中添加已安装的 JDK 17 根目录。
3. 将 `Project SDK` 设为 `17`，`Project language level` 设为 `17 - Sealed types, always-strict floating-point semantics`。
4. 打开 `Modules > personal-agent-backend`，将 `Module SDK` 设为 `Project SDK`，语言级别设为 17。
5. 打开 `Settings > Build, Execution, Deployment > Compiler > Java Compiler`，将模块字节码目标设为 `17`。
6. 打开 Maven 面板并点击 `Reload All Maven Projects`。

### 3. 确认 MySQL

本机 MySQL80 服务已经在运行。确认其中有数据库：

```sql
CREATE DATABASE IF NOT EXISTS work
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

只在确认 `work` 库没有需要保护的同名旧表后启动。应用启动时 Flyway 会自动执行 V1–V6。

### 4. 在 IDEA Terminal 启动后端

打开 IDEA 底部的 `Terminal`，执行：

```powershell
Set-Location D:\pp-program\Personal-Workspace\backend
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = Read-Host 'MySQL 密码'
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:MAIL_USERNAME = Read-Host 'QQ 发件邮箱；暂不测邮件可直接回车'
$env:MAIL_AUTH_CODE = Read-Host 'QQ 邮箱 SMTP 授权码；暂不测邮件可直接回车'
.\mvnw.cmd spring-boot:run
```

看到 `Started PersonalAgentApplication` 后，后端和内置 Tomcat 已运行在：

```text
http://127.0.0.1:8080
```

不要把数据库密码、JWT 密钥或邮箱授权码写进项目文件。

### 5. 在第二个 Terminal 启动前端

点击 Terminal 页签旁边的 `+` 新开一个终端：

```powershell
Set-Location D:\pp-program\Personal-Workspace\frontend
npm run dev
```

浏览器打开 Vite 输出的地址，通常是：

```text
http://127.0.0.1:5173
```

前端会把 `/api` 请求代理到 `127.0.0.1:8080`。

### 6. 最小检查

依次检查：

1. 打开 `/register`，输入邮箱并点击发送验证码。
2. 收到邮件后填写验证码、用户名和密码完成注册。
3. 登录。
4. 退出后打开 `/forgot-password`，发送验证码并重置密码。
5. 用新密码重新登录。

阿里百炼 Key 不是认证流程必需项。只有生成 AI 周报总结或处理课程录音时才需要 `DASHSCOPE_API_KEY`。

## 方式二：在 IDEA 中配置外置 Tomcat（非必需）

只有需要演示 WAR 部署时才用这一方式。

### 1. 前置条件

- 安装 Apache Tomcat 10.1，不能使用 Tomcat 9。
- IDEA 必须是 Ultimate 版；Community 版没有 Tomcat Server 运行配置。
- 先使用终端完成前端与 WAR 构建：

```powershell
Set-Location D:\pp-program\Personal-Workspace\frontend
npm run build
Set-Location ..\backend
.\mvnw.cmd clean package
```

WAR 位于：

```text
D:\pp-program\Personal-Workspace\backend\target\personal-agent.war
```

### 2. 添加 Tomcat

1. 打开 `File > Settings > Build, Execution, Deployment > Application Servers`。
2. 点击 `+`，选择 `Tomcat Server`。
3. `Tomcat Home` 选择 Tomcat 10.1 的解压目录。
4. 打开 `Run > Edit Configurations`。
5. 点击 `+ > Tomcat Server > Local`。
6. `Application server` 选择刚才添加的 Tomcat。
7. HTTP Port 使用 `8080`。
8. 在 `Deployment` 页添加 `personal-agent:war exploded`；如果旧 IDEA 无法生成 Artifact，直接把已构建的 `personal-agent.war` 放进 Tomcat 的 `webapps` 目录，再运行 Tomcat。
9. Application context 设置为 `/`。
10. 在运行配置的 `Environment variables` 中设置 `DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`MAIL_USERNAME`、`MAIL_AUTH_CODE`。

### 3. 启动

启动 Tomcat 配置后访问：

```text
http://127.0.0.1:8080/login
```

## 推荐的长期处理

项目已兼容 Java 17，可以在 IDEA 2021.3 中开发。当前电脑实际检测到的是 IDEA 2021.2；它也支持 Java 17，但如果你已经安装了 2021.3，请在 `Help > About` 确认启动的是新版本程序。
