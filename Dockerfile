# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home appuser \
 && mkdir -p /data && chown appuser:appuser /data
WORKDIR /app
COPY --from=build /app/target/gestion-productos.jar app.jar

USER appuser
# El archivo SQLite vive en /data. Monta un volumen aquí para conservarlo entre contenedores.
ENV DB_PATH=/data/productos.db
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
