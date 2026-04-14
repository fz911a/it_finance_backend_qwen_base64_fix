# IT 项目智能财务管理系统后端（千问已配置版）

这版已经帮你填好了千问兼容网关参数：
- Base URL 已配置
- API Key 已写入 application.yml
- 文本模型已配置
- 视觉模型已配置
- AI 已启用 enabled=true

## 当前 AI 配置
- provider: openai-compatible
- base-url: https://llm.chudian.site/v1
- endpoint: /chat/completions
- chat-model: qwen3.5-plus
- vision-model: qwen3.5-plus

## 启动前
1. 检查 MySQL 配置
2. 导入数据库：
```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```
3. 启动：
```bash
mvn spring-boot:run
```

## 说明
如果后续你发现 `qwen3.5-plus` 不支持视觉输入，只需要把 `vision-model` 改成你网关里支持图片输入的模型名即可，前端无需大改。


## 本次修复
- 已把本地 `/uploads/...` 图片自动转成 base64 Data URL 再发给模型
- 不再依赖模型网关去访问你的 `localhost` 图片地址
- 这样在本地 Windows / 小程序联调时更稳定
