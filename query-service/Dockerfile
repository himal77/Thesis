FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn -pl query-service -am clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /build/query-service/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]