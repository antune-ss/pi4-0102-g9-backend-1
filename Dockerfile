FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/co2nect-server-1.0-SNAPSHOT.jar app.jar
COPY certs/ ./certs/

ENV APP_PROFILE=cloud

ENTRYPOINT ["java", "-jar", "app.jar"]