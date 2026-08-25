# 周报与工作总结测试

## 自动化

```powershell
Set-Location backend
.\mvnw.cmd test
Set-Location ../frontend
npm test -- --run
npm run build
```

## 浏览器主流程

1. 登录后进入“结构化周报”。
2. 填写本周核心工作并保存；另外两栏可为空。
3. 按年份、月份查询，打开详情并修改。
4. 导入 DOCX，确认识别内容追加而不是覆盖已有文字。
5. 进入“工作总结”，选择季度或年度并生成。
6. 修改核心内容、日常工作和自我评分后保存。
7. 使用另一账号直接访问报告 ID，应返回 404。

未配置 `DASHSCOPE_API_KEY` 时，不执行真实模型生成测试。
