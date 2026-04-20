# Фитнес-клуб — Web Application

Spring Boot 3.x приложение для управления фитнес-клубом.

## Технологии
- **Backend**: Java 17, Spring Boot 3.2, Spring Security (JWT), Spring Data JPA
- **БД**: H2 (по умолчанию) / PostgreSQL
- **Миграции**: Flyway
- **API Docs**: Swagger UI — http://localhost:8080/swagger-ui.html
- **Frontend**: HTML + Bootstrap 5 + Vanilla JS (SPA-стиль)

## Роли и тестовые аккаунты

| Роль | Email | Пароль | Страница |
|------|-------|--------|---------|
| Администратор | admin@fit.com | admin | /admin.html |
| Ресепшен | reception@fit.com | reception | /reception.html |
| Тренер 1 | trainer1@fit.com | trainer1 | /trainer.html |
| Тренер 2 | trainer2@fit.com | trainer2 | /trainer.html |
| Клиент 1 | client1@fit.com | client1 | /client.html |
| Клиент 2 | client2@fit.com | client2 | /client.html |

## Запуск (H2 — без внешней БД)

```bash
# Требования: Java 17+
./gradlew bootRun
```

Приложение стартует на **http://localhost:8080**

- Страница входа: http://localhost:8080/index.html
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:petgymdb`)

## Запуск с PostgreSQL

1. Создайте базу данных:
```sql
CREATE DATABASE petgym;
```

2. Запустите с профилем postgres:
```bash
./gradlew bootRun --args='--spring.profiles.active=postgres'
```

Или настройте `application-postgres.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/petgym
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

## Сборка

```bash
./gradlew clean build
java -jar build/libs/Pet_Gym-1.0.0.jar
```

## Структура API

### Аутентификация
- `POST /api/auth/register` — регистрация клиента
- `POST /api/auth/login` — вход (возвращает JWT)

### Клиент (`/api/client/**`)
- Управление абонементами, бронирование тренировок, просмотр программы

### Ресепшен (`/api/reception/**`)
- Управление клиентами, оформление абонементов, отметка посещений

### Администратор (`/api/admin/**`)
- CRUD типов абонементов, финансовые отчёты, управление сотрудниками

### Тренер (`/api/trainer/**`)
- Расписание, управление бронированиями, создание программ тренировок

## Тесты

```bash
./gradlew test
```

## Архитектура

```
com.petgym/
├── config/        # SecurityConfig, SwaggerConfig
├── controller/    # REST контроллеры по ролям
├── domain/        # JPA сущности
├── dto/           # Data Transfer Objects
├── exception/     # Кастомные исключения + GlobalExceptionHandler
├── repository/    # Spring Data JPA репозитории
├── scheduled/     # Фоновые задачи (@Scheduled)
├── security/      # JWT, UserDetailsService
└── service/       # Бизнес-логика
```
