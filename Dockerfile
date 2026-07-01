# 第一阶段：使用 Maven 与 Java 25 构建 EcoVault。
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY . .
RUN mvn -q clean package -DskipTests

# 第二阶段：使用 Java 25 JRE 运行应用。
FROM eclipse-temurin:25-jre

ENV LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_OPTS="-Dfile.encoding=UTF-8" \
    SPRING_PROFILES_ACTIVE=prod

WORKDIR /app
COPY --from=build /workspace/target/ecovault.jar /app/ecovault.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/ecovault.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
