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
RUN useradd --system --uid 1001 restaurante
COPY --from=build /app/target/*.jar app.jar
USER restaurante
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]