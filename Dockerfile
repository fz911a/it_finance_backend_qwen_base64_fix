# 使用Java 17基础镜像
FROM eclipse-temurin:17-jdk-alpine

# 设置工作目录
WORKDIR /app

# 复制构建好的jar文件到容器中
COPY target/it-finance-backend-2.0.1.jar app.jar

# 复制数据库初始化脚本
COPY src/main/resources/sql/schema.sql /app/sql/

# 暴露端口
EXPOSE 8081

# 设置环境变量（默认值，可在运行时覆盖）
ENV DB_URL=jdbc:mysql://localhost:3306/it_finance_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
ENV DB_USERNAME=root
ENV DB_PASSWORD=
ENV AI_PROVIDER=openai-compatible
ENV AI_BASE_URL=https://llm.chudian.site/v1
ENV AI_API_KEY=
ENV AI_CHAT_MODEL=qwen3.5-plus
ENV AI_VISION_MODEL=qwen3.5-plus
ENV AI_ENABLED=true
ENV APP_SECURITY_JWT_SECRET=
ENV APP_SECURITY_TOKEN_EXPIRE_MINUTES=120
ENV APP_SECURITY_ALLOW_DEMO_LOGIN=false
ENV APP_IDEMPOTENCY_TTL_SECONDS=120

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
