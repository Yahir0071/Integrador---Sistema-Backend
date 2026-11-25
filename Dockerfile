# Etapa 1: Construcción usando Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución ligera
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENV SERVER_ADDRESS=0.0.0.0
ENV SERVER_PORT=8080
EXPOSE 8080
# Pasar en tiempo de ejecución: -e DB_PASSWORD=... -e JWT_SECRET=... -e ALLOWED_ORIGINS=...
ENTRYPOINT ["java", "-jar", "app.jar"]
