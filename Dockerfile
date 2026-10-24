# syntax=docker/dockerfile:1

# ---------- Estágio 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# O cache do BuildKit mantém ~/.m2 entre builds (dependências não são baixadas toda vez).
# Os testes rodam no CI (mvn verify); aqui só empacotamos.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# ---------- Estágio 2: runtime enxuto (somente JRE, usuário sem privilégios) ----------
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

RUN addgroup -S awos && adduser -S awos -G awos
COPY --from=build --chown=awos:awos /app/target/*.jar /app/app.jar
USER awos

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
