# IT Finance Backend

Spring Boot 后端，提供发票、付款、工资、报表、OCR、AI、风控和人脸相关接口。

## 目录说明

- `src/main/java`：业务代码
- `src/main/resources/application.yml`：数据库、Redis、AI 和安全配置
- `src/main/resources/sql/schema.sql`：初始化建表和演示数据
- `docs/`：补充文档
- `uploads/`：运行时上传文件目录

## 本地启动

1. 准备 MySQL 和 Redis
2. 导入数据库：

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```

3. 启动服务：

```bash
mvn spring-boot:run
```

Windows 下也可以直接运行 `run.bat`。

## 配置项

- `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`
- `AI_PROVIDER`、`AI_BASE_URL`、`AI_API_KEY`
- `APP_SECURITY_JWT_SECRET`

`application.yml` 里保留了默认值，正式环境建议全部改成环境变量。

## 说明

- 上传图片会落到 `uploads/`
- 如果要接入新的视觉模型，只需要修改 `AI_VISION_MODEL`
- `docs/AI_INTEGRATION.md` 里有 AI 接入说明
