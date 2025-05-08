# 第一阶段：构建阶段，使用 Maven 镜像
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

# 设置工作目录
WORKDIR /build

# 拷贝 Maven 配置（包含私有仓库等信息）
RUN mkdir -p /root/.m2/
COPY docs/maven/settings.xml /root/.m2/settings.xml

# 提前拷贝 pom.xml 和相关 metadata 文件（用于依赖缓存）
COPY pom.xml ./
COPY */pom.xml ./

# 下载依赖（缓存层关键步骤）
RUN mvn dependency:go-offline -B

# 拷贝源码
COPY . .

# 构建项目，跳过测试
RUN mvn -B -DskipTests package

# 拷贝目标 JAR（可根据模块名过滤）
RUN cp $(find . -type f -name "*PulsarRPAPro*.jar" | head -n 1) /build/app.jar

# ================================
# 第二阶段：运行时镜像
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 拷贝构建产物
COPY --from=builder /build/app.jar app.jar

# 设置环境变量
ENV JAVA_OPTS="-Xms2G -Xmx10G -XX:+UseG1GC"

EXPOSE 8082
EXPOSE 8182

# 创建非 root 用户运行
RUN addgroup --system --gid 1001 appuser \
 && adduser --system --uid 1001 --ingroup appuser appuser

RUN chown -R appuser:appuser /app
USER appuser

LABEL maintainer="Vincent Zhang <ivincent.zhang@gmail.com>"
LABEL description="PulsarRPAPro - Fully automated and hands-free, accurately extracting and understanding web content — powered by machine learning agents."

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar serve --server.port=${SERVER_PORT:-8082}"]
