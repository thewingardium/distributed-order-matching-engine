# Build Stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/distributed-order-matching-engine-0.0.1-SNAPSHOT.jar app.jar

# Expose Port
EXPOSE 8080

# Environment Variables
ENV JAVA_OPTS="-XX:+UseG1GC -Xmx512m -Xms256m"

# Run
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
