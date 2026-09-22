# ==============================================================================
# Dockerfile Multiestágio para o Backend (tcc-server)
# Plataforma: Java 25 & Spring Boot 4.0.6
# Otimizado para cache de camadas, segurança com usuário não-root e JVM container-aware
# ==============================================================================

# --- Estágio 1: Compilação e Empacotamento (Build) ---
FROM eclipse-temurin:25-jdk-alpine AS builder

# Instala o Maven no Alpine
RUN apk add --no-cache maven

WORKDIR /build

# 1. Copia primeiro apenas o POM para cachear o download das dependências do Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# 2. Copia o código-fonte e compila gerando o JAR executável
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Estágio 2: Execução em Produção (Runtime) ---
FROM eclipse-temurin:25-jre-alpine AS runtime

# Cria usuário e grupo não-root para execução segura do container
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copia o artefato compilado do estágio de build
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

# Define usuário de execução não-root
USER appuser

# Expõe a porta padrão do servidor Spring Boot
EXPOSE 8080

# Flags da JVM otimizadas para containers (G1GC + limitação inteligente de memória RAM)
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
