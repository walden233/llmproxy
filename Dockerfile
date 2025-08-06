# --- Stage 1: Build the application ---
# 使用一个包含 Maven 和 JDK 的镜像作为构建环境
FROM maven:3.8.5-openjdk-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 并下载依赖，利用 Docker 的层缓存机制
COPY pom.xml .
RUN mvn dependency:go-offline

# 复制所有源代码
COPY src ./src

# 执行 Maven 打包命令
RUN mvn clean package -DskipTests

# --- Stage 2: Create the final, smaller image ---
# 使用一个仅包含 JRE 的更小的基础镜像
FROM eclipse-temurin:17-jre

# 设置工作目录
WORKDIR /app

# 从构建阶段复制打包好的 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 暴露 Spring Boot 应用的端口
EXPOSE 8060

# 设置容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]