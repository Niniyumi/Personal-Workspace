# IDEA + 可执行 JAR

项目使用 Java 17 和 Spring Boot 内置 Tomcat，不需要在 IDEA 中配置外置 Tomcat。

## IDEA 直接运行

1. `File > Project Structure` 将 Project SDK 和模块 SDK 都设为 Java 17。
2. `Run > Edit Configurations` 新建 `Spring Boot` 或 `Application`。
3. Main class 选择 `com.niniyumi.personalagent.PersonalAgentApplication`。
4. JRE 选择 Java 17。
5. 二选一配置运行参数：
   - 继续使用环境变量：在 `Environment variables` 填写 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`MAIL_USERNAME`、`MAIL_AUTH_CODE`、`DASHSCOPE_API_KEY`、`COURSE_STORAGE_DIR`、`FFMPEG_PATH` 和 `FFPROBE_PATH`。
   - 使用项目根目录中已创建的 `application-local.yml`：在 `Program arguments` 填写 `--spring.config.additional-location=file:D:/pp-program/Personal-Workspace/application-local.yml`。
6. 启动后访问 `http://localhost:8080/`。

## 构建一个可部署文件

```powershell
Set-Location D:\pp-program\Personal-Workspace\frontend
npm run build
Set-Location ..\backend
.\mvnw.cmd clean package
java -jar target\personal-agent.jar --spring.config.additional-location=file:D:/pp-program/Personal-Workspace/application-local.yml
```

`personal-agent.jar` 已包含 Vue 页面、后端和内置 Tomcat。本机的 `application-local.yml`、`application-secret.yml`、密码和密钥不会进入 JAR。前端代码变化后要先重新执行 `npm run build`，再重新打包 JAR。

课程录音需要服务器或本机安装 FFmpeg，并让 `FFMPEG_PATH`、`FFPROBE_PATH` 指向真实程序。日志默认写入运行目录下的 `logs/personal-agent.log`，也可以用 `LOG_FILE` 指定固定路径。
