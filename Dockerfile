# syntax=docker/dockerfile:1

# ===== Stage 1: build =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache de dependencias entre builds (BuildKit): sobrevive a mudancas no pom.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

# Codigo + empacotamento. Testes (ArchUnit + dominio) rodam aqui: nao precisam de infra,
# entao o build falha rapido se o hexagono vazar (P1) ou invariantes quebrarem.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package

# ===== Stage 2: runtime =====
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl para o healthcheck do compose
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/pix-poc-*.jar app.jar

EXPOSE 8080

# Heap e flags controlados via JAVA_TOOL_OPTIONS (definido no docker-compose)
ENTRYPOINT ["java", "-jar", "app.jar"]
