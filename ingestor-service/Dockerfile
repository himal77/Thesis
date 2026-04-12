FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn -pl ingestor-service -am clean package -DskipTests


FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /build/ingestor-service/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]