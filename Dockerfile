FROM openjdk:17-jdk-slim

WORKDIR /app

# Set JVM options for containers and Mithra generation
ENV JAVA_OPTS="-Djava.awt.headless=true -Xmx1g -XX:+UseContainerSupport"
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Djava.awt.headless=true -Xmx1g"

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew

# Create necessary directories for Mithra code generation
RUN mkdir -p src/main/generated-java
RUN mkdir -p src/main/resources/generated-db/sql

COPY src src

# Generate Mithra sources and build
RUN ./gradlew mithraGenerateSources --no-daemon --stacktrace --max-workers=1
RUN ./gradlew build -x test --no-daemon --max-workers=1

EXPOSE 8080

CMD ["java", "-jar", "build/libs/fxtrade-0.0.1-SNAPSHOT.jar"]