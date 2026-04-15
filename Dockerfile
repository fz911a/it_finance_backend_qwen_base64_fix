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

# 设置非敏感的环境变量
ENV DB_URL=jdbc:mysql://localhost:3306/it_finance_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai \
    DB_USERNAME=root \
    AI_PROVIDER=openai-compatible \
    AI_BASE_URL=https://llm.chudian.site/v1 \
    AI_CHAT_MODEL=qwen3.5-plus \
    AI_VISION_MODEL=qwen3.5-plus \
    AI_ENABLED=true \
    APP_SECURITY_TOKEN_EXPIRE_MINUTES=120 \
    APP_SECURITY_ALLOW_DEMO_LOGIN=false \
    APP_IDEMPOTENCY_TTL_SECONDS=120

# 注意：敏感信息（DB_PASSWORD, AI_API_KEY, APP_SECURITY_JWT_SECRET）
# 必须在容器运行时通过 -e 或其他方式指定，不应该在镜像中设置

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
