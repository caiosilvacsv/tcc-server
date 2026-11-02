# syntax=docker/dockerfile:1
# ==============================================================================
# Dockerfile Multiestágio Ultra-Enxuto para o Backend (tcc-server)
# Plataforma: Java 25 & Spring Boot 4.0.6
# Otimizado com Cache Mount do Maven, Layered Extraction e JVM enxuta
# ==============================================================================

# --- Estágio 1: Compilação, Empacotamento e Extração de Camadas (Build) ---
FROM eclipse-temurin:25-jdk-alpine AS builder

# Instala o Maven no Alpine e limita uso de RAM durante o build para não sobrecarregar o Docker
RUN apk add --no-cache maven
ENV MAVEN_OPTS="-Xmx512m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

WORKDIR /build

# Compila usando Cache Mount do BuildKit (baixa apenas o necessário e mantém o cache em /root/.m2)
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B -q && \
    mv target/*.jar app.jar && \
    java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# --- Estágio 2: Execução em Produção Ultra-Leve (Runtime) ---
FROM eclipse-temurin:25-jre-alpine AS runtime

# Cria usuário e grupo de sistema não-root para isolamento de segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copia as camadas na ordem de menor para maior frequência de alteração (maximiza cache Docker)
COPY --from=builder --chown=appuser:appgroup /build/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /build/extracted/application/ ./

# Define usuário de execução não-root
USER appuser

# Expõe a porta interna do servidor Spring Boot
EXPOSE 8080

# Flags da JVM otimizadas para baixo consumo de RAM e boot rápido em containers
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:+UseStringDeduplication", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Xss256k", \
  "-XX:+TieredCompilation", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
