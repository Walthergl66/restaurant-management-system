# Build stage: JDK 25 + Maven Wrapper
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B clean package -DskipTests

# Runtime stage: JRE 25, usuario no root
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 restaurante
COPY --from=build /app/target/*.jar app.jar
USER restaurante
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --retries=10 --start-period=40s CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]