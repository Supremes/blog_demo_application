# 使用多阶段构建
# 阶段1: 构建
FROM maven:3.8.7-eclipse-temurin-17 AS build
WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY ../pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY ../src ./src
RUN mvn clean package -DskipTests

# 阶段2: 运行
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 复制构建好的 jar 文件
COPY --from=build /app/target/demoApplication-1.0-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8080

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]