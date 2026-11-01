# ==============================================================================
# Dockerfile Multiestágio Ultra-Enxuto para o Backend (tcc-server)
# Plataforma: Java 25 & Spring Boot 4.0.6
# Otimizado com Layered Extraction (jarmode=tools), usuário não-root e JVM enxuta
# ==============================================================================

# --- Estágio 1: Compilação, Empacotamento e Extração de Camadas (Build) ---
FROM eclipse-temurin:25-jdk-alpine AS builder

# Instala o Maven no Alpine sem manter cache de índices
RUN apk add --no-cache maven

WORKDIR /build

# 1. Copia primeiro apenas o POM para cachear o download das dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# 2. Copia o código-fonte, compila o JAR e extrai em camadas descompactadas
COPY src ./src
RUN mvn clean package -DskipTests -B && \
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

# Flags da JVM otimizadas para baixo consumo de RAM e boot rápido em containers:
# -XX:+UseG1GC -XX:+UseStringDeduplication : Coletor G1 com desduplicação automática de Strings em memória
# -XX:MaxRAMPercentage=75.0                : Respeita limites do container
# -Xss256k                                 : Reduz pilha por thread de 1MB para 256KB (economiza ~45MB de RAM nativa)
# -XX:+TieredCompilation                   : Otimiza compilação JIT
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:+UseStringDeduplication", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Xss256k", \
  "-XX:+TieredCompilation", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
