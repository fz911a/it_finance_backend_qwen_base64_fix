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

# 设置环境变量（在运行时通过外部传入）
ENV DB_URL
ENV DB_USERNAME
ENV DB_PASSWORD
ENV AI_PROVIDER
ENV AI_BASE_URL
ENV AI_API_KEY
ENV AI_CHAT_MODEL
ENV AI_VISION_MODEL
ENV AI_ENABLED
ENV APP_SECURITY_JWT_SECRET
ENV APP_SECURITY_TOKEN_EXPIRE_MINUTES
ENV APP_SECURITY_ALLOW_DEMO_LOGIN
ENV APP_IDEMPOTENCY_TTL_SECONDS

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
