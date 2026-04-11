# -------- Stage 1: Build --------
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy entire project (needed for commons-core)
COPY .. .

# Build only processor-service + required modules
RUN mvn -pl processor-service -am clean package -DskipTests

# -------- Stage 2: Runtime --------
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy built jar from builder
COPY --from=builder /build/processor-service/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]