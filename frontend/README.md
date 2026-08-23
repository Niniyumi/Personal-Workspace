# Personal Agent 前端

Vue 3 + TypeScript + Vite 单页应用。当前提供注册、登录、会话恢复、退出和受保护的个人工作台。

## 本地启动

先确保后端运行在 `http://127.0.0.1:8080`，再执行：

```powershell
npm install
npm run dev
```

浏览器访问 `http://127.0.0.1:5173`。开发服务器会把 `/api` 请求代理到后端，因此前端不需要保存数据库或 JWT 配置。

## 自查命令

```powershell
npm run test -- --run
npm run build
```

详细测试场景见 [`../docs/testing/login-ui-test-cases.md`](../docs/testing/login-ui-test-cases.md)。
