# ==============================================================================
# Dockerfile Multiestágio para o backend do TCC (tcc-server)
# Garante a compilação e execução seguras usando Java 25 no Render/nuvem
# ==============================================================================

# --- Estágio 1: Compilação (Build) ---
FROM maven:3.9.9-eclipse-temurin-25-alpine AS builder
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
