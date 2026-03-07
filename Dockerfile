# -------- Stage 1: Build the application --------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom and download dependencies first (better caching)
COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests


# -------- Stage 2: Runtime image --------
FROM eclipse-temurin:17-jdk-jammy

# Create non-root user for security
RUN useradd -m springuser

WORKDIR /app

# Copy jar from builder
COPY --from=builder /build/target/*.jar app.jar

# Set ownership
RUN chown springuser:springuser app.jar

USER springuser

# JVM tuning for small servers
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]