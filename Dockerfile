# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM gradle:8.7-jdk21 AS builder

WORKDIR /app

# Копируем только файлы сборки — слой кешируется до изменения зависимостей
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Скачиваем зависимости (кешируется отдельно)
RUN gradle dependencies --no-daemon || true

# Копируем исходники и собираем
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Нескомпрометированный пользователь (не root)
RUN addgroup -S petgym && adduser -S petgym -G petgym
USER petgym

# Копируем только JAR из builder-стадии
COPY --from=builder /app/build/libs/Pet_Gym-1.0.0.jar app.jar

# Переменные окружения по умолчанию (переопределяются в docker-compose)
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/petgym \
    SPRING_DATASOURCE_USERNAME=postgres \
    SPRING_DATASOURCE_PASSWORD=1234 \
    SERVER_PORT=8091 \
    JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8091

# Healthcheck — проверяем /swagger-ui.html раз в 30 сек
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8091/swagger-ui.html || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
