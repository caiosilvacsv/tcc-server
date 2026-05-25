# ==============================================================================
# Dockerfile para o backend do TCC (tcc-server)
# ==============================================================================
# Baseado na JRE 25 (Java Runtime Environment) sobre Alpine Linux para garantir
# uma imagem extremamente leve, segura e otimizada para o deploy de produção.
# ==============================================================================
FROM eclipse-temurin:25-jre-alpine

# Define o diretório de trabalho interno do container
WORKDIR /app

# Copia o JAR empacotado pelo Maven (target/) para dentro do container
# Dica: Execute 'mvn clean package -DskipTests' localmente antes de rodar o docker-build
COPY target/tcc-server-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta lógica padrão do Spring Boot
EXPOSE 8080

# Define a instrução de inicialização do container Java com otimizações de memória
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "app.jar"]
