## --- Stage 1: Build the application ---
FROM maven:3.8.5-openjdk-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src

RUN mvn -B -ntp clean package -DskipTests \
    && cp $(find target -maxdepth 1 -type f -name "*.jar" ! -name "*original*.jar") app.jar

## --- Stage 2: Lightweight runtime image ---
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/app.jar app.jar

EXPOSE 8060

ENTRYPOINT ["java", "-jar", "app.jar"]
