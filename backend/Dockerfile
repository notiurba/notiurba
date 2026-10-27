# Build con Maven y Java 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Runtime con JRE ligero (sin JDK/Maven)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

ARG PORT=8080
ENV PORT=${PORT}
EXPOSE ${PORT}

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
