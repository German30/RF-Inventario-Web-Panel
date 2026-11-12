# =====================================================================
# RF Inventario Panel API - imagen de produccion
#
#   docker build -t rf-inventario-panel-api .
#
# Dos etapas: se compila con Maven y se corre solo con el JRE.
# =====================================================================

# ---------------------------------------------------------------------
# Etapa 1: compilar
# ---------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests

RUN java -Djarmode=tools -jar target/*.jar extract --destination extracted
RUN mv extracted/*.jar extracted/app.jar

# ---------------------------------------------------------------------
# Etapa 2: ejecutar
# ---------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system rf && useradd --system --gid rf --home /app rf

COPY --from=build --chown=rf:rf /build/extracted/lib ./lib
COPY --from=build --chown=rf:rf /build/extracted/app.jar ./app.jar

USER rf

EXPOSE 8082

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
    CMD curl -fsS http://localhost:${SERVER_PORT:-8082}/api/ping || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
