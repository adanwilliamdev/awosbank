# AWOS Bank — imagem única para Vercel (Container Runtime), Docker Compose e qualquer host de containers.

# ---------- Estágio 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Camada de dependências separada: só é refeita quando o pom.xml muda (não depende de cache mount do BuildKit,
# que nem todo builder remoto oferece). Os testes rodam no CI (mvn verify); aqui só empacotamos.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package \
    && java -Djarmode=layertools -jar target/*.jar extract --destination /app/layers

# ---------- Estágio 2: runtime enxuto (somente JRE, usuário sem privilégios) ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S awos && adduser -S awos -G awos

# Camadas do Spring Boot, da que menos muda para a que mais muda: melhor cache de imagem e pull mais rápido.
COPY --from=build --chown=awos:awos /app/layers/dependencies/ ./
COPY --from=build --chown=awos:awos /app/layers/spring-boot-loader/ ./
COPY --from=build --chown=awos:awos /app/layers/snapshot-dependencies/ ./
COPY --from=build --chown=awos:awos /app/layers/application/ ./
USER awos

# Ajustes para partida a frio rápida e pouca memória (instâncias serverless escalam a zero):
#  - SerialGC + TieredStopAtLevel=1: JVM sobe bem mais rápido e usa menos RAM (suficiente para esta carga);
#  - -Xshare:auto: usa o CDS padrão do JDK;
#  - spring.jmx.enabled=false: sem custo de JMX.
ENV JAVA_OPTS="-XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xshare:auto -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Dspring.jmx.enabled=false" \
    SPRING_PROFILES_ACTIVE=prod \
    PORT=8080
EXPOSE 8080

# Usado pelo Docker Compose; o Vercel ignora HEALTHCHECK.
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://127.0.0.1:${PORT}/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
