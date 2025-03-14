# 使用 Java 8 镜像
FROM openjdk:8-jre-slim

# 设置工作目录
WORKDIR /app

# 将构建好的 JAR 文件复制到容器中
COPY target/web-1.0-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]