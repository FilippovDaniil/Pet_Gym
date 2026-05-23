# ──────────────────────────────────────────────────────────────────────────────
# Dockerfile — Multi-stage сборка Spring Boot приложения Pet Gym
#
# Multi-stage = два FROM в одном файле:
#   Stage 1 (builder): JDK + Gradle — собираем JAR (тяжёлый, ~800MB)
#   Stage 2 (runtime): только JRE — запускаем JAR (лёгкий, ~200MB)
#
# Итоговый образ НЕ содержит Gradle, JDK, исходники — только JAR и JRE.
# Это уменьшает поверхность атаки и размер образа.
# ──────────────────────────────────────────────────────────────────────────────

# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# gradle:8.7-jdk21 — официальный образ с Gradle 8.7 и OpenJDK 21.
# JDK 21 компилирует Java 17 код (sourceCompatibility = JavaVersion.VERSION_17 в build.gradle).
# "AS builder" — имя стадии. Используется в Stage 2: COPY --from=builder
FROM gradle:8.7-jdk21 AS builder

# Рабочая директория внутри контейнера.
# Все последующие команды выполняются в /app.
# Если /app не существует — Docker создаёт автоматически.
WORKDIR /app

# ── Оптимизация кеширования Docker слоёв ──────────────────────────────────────
# Docker кеширует каждый слой (инструкцию) ОТДЕЛЬНО.
# Слой пересобирается только если изменились файлы в COPY.
#
# СТРАТЕГИЯ: копируем файлы зависимостей ОТДЕЛЬНО от исходного кода.
# Зависимости меняются редко → слой с gradle dependencies кешируется долго.
# Исходники меняются часто → только их слой пересобирается при изменениях.

# Копируем ТОЛЬКО файлы описания зависимостей (build.gradle, settings.gradle)
# и Gradle wrapper (gradlew + gradle/wrapper/).
# Эти файлы меняются редко — слой будет кешироваться пока они не изменятся.
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Скачиваем все зависимости из Maven Central в локальный кеш Gradle.
# "|| true" — не падать если команда вернёт ненулевой код.
# (gradle dependencies иногда возвращает предупреждения как ошибки)
# После этого шага все JAR зависимости в ~/.gradle/caches/ → кешируются в Docker слое.
RUN gradle dependencies --no-daemon || true

# Копируем исходный код приложения.
# Этот слой пересобирается при каждом изменении src/.
# Но предыдущий слой (зависимости) остаётся кешированным!
COPY src ./src

# Собираем Spring Boot JAR.
# bootJar: Gradle таск из spring-boot plugin — создаёт исполняемый fat-JAR.
# --no-daemon: не запускать Gradle daemon (в Docker не нужен, экономим память).
# -x test: пропускаем тесты при сборке (ускоряет сборку образа).
# Результат: build/libs/Pet_Gym-1.0.0.jar (имя берётся из build.gradle: version='1.0.0')
RUN gradle bootJar --no-daemon -x test

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
# eclipse-temurin:21-jre-alpine — только JRE (не JDK!), Alpine Linux.
# JRE = Java Runtime Environment (без компилятора javac, без Gradle).
# Alpine Linux = минимальный дистрибутив (~5MB vs Ubuntu ~80MB).
# eclipse-temurin = официальный дистрибутив OpenJDK от Eclipse Foundation.
FROM eclipse-temurin:21-jre-alpine

# Рабочая директория в финальном образе.
WORKDIR /app

# ── Безопасность: непривилегированный пользователь ───────────────────────────
# По умолчанию контейнер запускается от root (UID 0) — это небезопасно.
# Создаём группу petgym и пользователя petgym.
# addgroup -S: системная группа (без домашней директории, без shell)
# adduser -S: системный пользователь (без домашней директории, без пароля)
RUN addgroup -S petgym && adduser -S petgym -G petgym

# Переключаемся на непривилегированного пользователя.
# Все последующие команды и запуск контейнера — от petgym, не от root.
USER petgym

# Копируем ТОЛЬКО JAR из Stage 1 (builder).
# --from=builder: берём файл из первой стадии сборки (по имени "builder").
# Gradle создаёт JAR с именем <rootProject.name>-<version>.jar.
# rootProject.name = 'Pet_Gym' (settings.gradle), version = '1.0.0' (build.gradle).
COPY --from=builder /app/build/libs/Pet_Gym-1.0.0.jar app.jar

# ── Переменные окружения по умолчанию ─────────────────────────────────────────
# Эти значения используются при запуске БЕЗ docker-compose / K8s ConfigMap.
# В K8s они ПЕРЕОПРЕДЕЛЯЮТСЯ через ConfigMap (envFrom: configMapRef: pet-gym-config).
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/petgym \
    SPRING_DATASOURCE_USERNAME=postgres \
    SPRING_DATASOURCE_PASSWORD=1234 \
    SERVER_PORT=8091 \
    JAVA_OPTS="-Xms256m -Xmx512m"
# SPRING_DATASOURCE_URL: "db" — имя сервиса в docker-compose (для локального запуска)
# В K8s заменяется на: jdbc:postgresql://postgres-service:5432/petgym
# JAVA_OPTS: ограничение JVM heap (256MB-512MB) — передаётся в ENTRYPOINT

# Документируем порт (не открывает реально — только информация для docker inspect)
EXPOSE 8091

# ── Healthcheck для docker-compose ───────────────────────────────────────────
# K8s использует readinessProbe/livenessProbe из манифеста (06-app.yaml).
# HEALTHCHECK используется только docker-compose (docker-compose.yml) и `docker run`.
# wget -qO-: тихий (-q) запрос, вывод в stdout (-O-). Если код != 200 → exit 1.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8091/swagger-ui.html || exit 1

# ── Команда запуска контейнера ─────────────────────────────────────────────────
# sh -c "..." — запускаем через shell чтобы переменная $JAVA_OPTS раскрылась.
# Если бы писали ENTRYPOINT ["java", "$JAVA_OPTS", "-jar", "app.jar"] —
# $JAVA_OPTS не раскрылся бы (exec form не использует shell).
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
