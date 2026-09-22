# ==============================================================================
# Dockerfile Multiestágio para o backend do TCC
# Garante a compilação e execução seguras usando Java 25 no Render/nuvem
# ==============================================================================

# --- Estágio 1: Compilação (Build) ---
FROM eclipse-temurin:25-jdk-alpine AS builder

# Instala o Maven leve no Alpine
RUN apk add --no-cache maven

WORKDIR /build

# Copia os arquivos de configuração do Maven e o código-fonte
COPY pom.xml .
COPY src ./src

# Compila o projeto e empacota o JAR pulando os testes unitários no deploy
RUN mvn clean package -DskipTests

# --- Estágio 2: Execução (Runtime) ---
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copia apenas o JAR compilado do estágio anterior para manter a imagem leve
COPY --from=builder /build/target/tcc-server-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta padrão do Spring Boot
EXPOSE 8080

# Comando de inicialização otimizado para produção
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "app.jar"]
