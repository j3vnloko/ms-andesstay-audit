# ---- Build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home appuser
COPY --from=build /app/target/ms-andesstay-audit-0.0.1-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8083
# Heap acotado: en la EC2 comparte memoria con Kafka, Prometheus, etc.
ENV JAVA_OPTS="-Xms128m -Xmx384m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
