# Фитнес-клуб — Учебный проект на Spring Boot

Полноценное веб-приложение для управления фитнес-клубом с бассейном.
Проект охватывает все основные концепции Spring Boot: безопасность, JPA, миграции БД, REST API, фоновые задачи, статический фронтенд.

---

## Содержание

1. [Технологический стек](#1-технологический-стек)
2. [Архитектура приложения](#2-архитектура-приложения)
3. [Структура проекта](#3-структура-проекта)
4. [База данных — схема и сущности](#4-база-данных--схема-и-сущности)
5. [Безопасность и JWT](#5-безопасность-и-jwt)
6. [REST API — все эндпоинты](#6-rest-api--все-эндпоинты)
7. [Бизнес-логика (сервисный слой)](#7-бизнес-логика-сервисный-слой)
8. [Фоновые задачи](#8-фоновые-задачи)
9. [Фронтенд](#9-фронтенд)
10. [Запуск проекта](#10-запуск-проекта)
11. [Docker и Docker Compose](#11-docker-и-docker-compose)
12. [Тестирование через Postman](#12-тестирование-через-postman)
13. [Тестовые аккаунты](#13-тестовые-аккаунты)
14. [Частые ошибки и их решения](#14-частые-ошибки-и-их-решения)

---

## 1. Технологический стек

| Компонент | Технология | Версия |
|-----------|-----------|--------|
| Язык | Java | 17+ (проект работает на 21) |
| Фреймворк | Spring Boot | 3.4.4 |
| Веб-слой | Spring MVC (REST) | — |
| Безопасность | Spring Security + JWT | — |
| ORM | Spring Data JPA (Hibernate) | — |
| СУБД | PostgreSQL | 17–18 |
| Миграции БД | Flyway | 10.21.0 |
| Документация API | Springdoc OpenAPI (Swagger UI) | 2.4.0 |
| Токены | jjwt (io.jsonwebtoken) | 0.12.5 |
| Генерация кода | Lombok | — |
| Сборка | Gradle | 9.1.0 |
| Контейнеризация | Docker + Docker Compose | — |

> **Почему Gradle 9.1 + Spring Boot 3.4.4?**
> Gradle 9.x сломал совместимость с plugin `io.spring.dependency-management` версий ниже 1.1.7.
> Spring Boot 3.4.x + dependency-management 1.1.7 — минимальная совместимая комбинация.

---

## 2. Архитектура приложения

Приложение построено по классической **многослойной архитектуре**:

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────────────┐
│              Security Filter Chain                  │
│  JwtAuthenticationFilter → проверяет Bearer токен   │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│            Controller Layer  (REST API)              │
│  AuthController, ClientController, AdminController,  │
│  ReceptionController, TrainerController              │
│  — принимает HTTP запросы                            │
│  — валидирует DTO через @Valid                       │
│  — делегирует бизнес-логику в Service                │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│             Service Layer (бизнес-логика)            │
│  AuthService, MembershipService, BookingService,     │
│  WorkoutService, VisitService, ReportService,        │
│  NotificationService, UserService                    │
│  — @Transactional — управляет транзакциями           │
│  — бросает кастомные исключения                      │
│  — маппит Entity → DTO вручную                       │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│           Repository Layer  (Spring Data JPA)        │
│  UserRepository, PurchaseRepository,                 │
│  TrainingBookingRepository, ...                      │
│  — интерфейсы, реализацию генерирует Spring          │
│  — кастомные @Query для сложных запросов             │
└────────────────────────┬────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│                  PostgreSQL                          │
│  Схема управляется Flyway-миграциями                 │
└─────────────────────────────────────────────────────┘
```

### Дополнительные компоненты

- **`config/`** — `SecurityConfig` (правила доступа), `SwaggerConfig` (Swagger UI), `DataInitializer` (тестовые данные)
- **`scheduled/`** — `ScheduledTasks` — фоновые задачи по cron-расписанию
- **`exception/`** — `GlobalExceptionHandler` — единая обработка всех ошибок через `@RestControllerAdvice`
- **`security/`** — JWT-фильтр, UserDetailsService, провайдер токенов

---

## 3. Структура проекта

```
Pet_Gym/
├── src/
│   ├── main/
│   │   ├── java/com/petgym/
│   │   │   ├── PetGymApplication.java          # Точка входа (@SpringBootApplication + @EnableScheduling)
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── DataInitializer.java        # Создаёт тестовых пользователей при старте
│   │   │   │   ├── SecurityConfig.java         # Правила Spring Security + CORS
│   │   │   │   └── SwaggerConfig.java          # Настройка OpenAPI / Swagger UI
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java         # POST /api/auth/register, /login
│   │   │   │   ├── ClientController.java       # GET/POST /api/client/**
│   │   │   │   ├── ReceptionController.java    # GET/POST /api/reception/**
│   │   │   │   ├── AdminController.java        # CRUD   /api/admin/**
│   │   │   │   └── TrainerController.java      # GET/PUT /api/trainer/**
│   │   │   │
│   │   │   ├── domain/                         # JPA-сущности (таблицы БД)
│   │   │   │   ├── User.java
│   │   │   │   ├── Client.java                 # 1:1 с User, доп. поля клиента
│   │   │   │   ├── Trainer.java                # 1:1 с User, специализация
│   │   │   │   ├── MembershipType.java         # Тип абонемента
│   │   │   │   ├── Purchase.java               # Факт покупки абонемента
│   │   │   │   ├── TrainingBooking.java        # Бронирование тренировки
│   │   │   │   ├── WorkoutProgram.java         # Программа тренировок
│   │   │   │   ├── WorkoutExercise.java        # Упражнение в программе
│   │   │   │   ├── Visit.java                  # Отметка о посещении
│   │   │   │   ├── Notification.java           # Внутреннее уведомление
│   │   │   │   ├── Role.java                   # enum: CLIENT, TRAINER, RECEPTION, ADMIN
│   │   │   │   └── BookingStatus.java          # enum: PENDING, CONFIRMED, CANCELLED_*, COMPLETED
│   │   │   │
│   │   │   ├── dto/                            # Data Transfer Objects (не сущности!)
│   │   │   │   ├── auth/
│   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   └── AuthResponse.java
│   │   │   │   ├── report/
│   │   │   │   │   └── RevenueReportDto.java
│   │   │   │   ├── BookingDto.java
│   │   │   │   ├── ClientDto.java
│   │   │   │   ├── CreateBookingRequest.java
│   │   │   │   ├── ErrorResponse.java          # Стандартный формат ошибок
│   │   │   │   ├── MembershipTypeDto.java
│   │   │   │   ├── NotificationDto.java
│   │   │   │   ├── PurchaseDto.java
│   │   │   │   ├── TrainerDto.java
│   │   │   │   ├── UserDto.java
│   │   │   │   ├── VisitDto.java
│   │   │   │   ├── WorkoutExerciseDto.java
│   │   │   │   └── WorkoutProgramDto.java
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java     # @RestControllerAdvice
│   │   │   │   ├── BookingConflictException.java   # 409 Conflict
│   │   │   │   ├── BusinessException.java          # 400 Bad Request (общая)
│   │   │   │   ├── InvalidCancellationException.java # 400
│   │   │   │   ├── MembershipExpiredException.java # 403 Forbidden
│   │   │   │   └── ResourceNotFoundException.java  # 404 Not Found
│   │   │   │
│   │   │   ├── repository/                     # Spring Data JPA интерфейсы
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── ClientRepository.java
│   │   │   │   ├── TrainerRepository.java
│   │   │   │   ├── MembershipTypeRepository.java
│   │   │   │   ├── PurchaseRepository.java
│   │   │   │   ├── TrainingBookingRepository.java
│   │   │   │   ├── WorkoutProgramRepository.java
│   │   │   │   ├── WorkoutExerciseRepository.java
│   │   │   │   ├── VisitRepository.java
│   │   │   │   └── NotificationRepository.java
│   │   │   │
│   │   │   ├── scheduled/
│   │   │   │   └── ScheduledTasks.java         # Cron-задачи
│   │   │   │
│   │   │   ├── security/
│   │   │   │   ├── JwtTokenProvider.java       # Создание/валидация JWT
│   │   │   │   ├── JwtAuthenticationFilter.java# OncePerRequestFilter
│   │   │   │   └── UserDetailsServiceImpl.java # Загрузка пользователя для Spring Security
│   │   │   │
│   │   │   └── service/
│   │   │       ├── AuthService.java
│   │   │       ├── BookingService.java
│   │   │       ├── MembershipService.java
│   │   │       ├── NotificationService.java
│   │   │       ├── ReportService.java
│   │   │       ├── UserService.java
│   │   │       ├── VisitService.java
│   │   │       └── WorkoutService.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties          # Основной конфиг (PostgreSQL, JWT, порт)
│   │       ├── application-postgres.properties # Профиль для другого сервера PostgreSQL
│   │       ├── db/migration/
│   │       │   ├── V1__init.sql               # Создание всех таблиц
│   │       │   └── V2__data.sql               # Начальные типы абонементов
│   │       └── static/                        # Фронтенд (HTML+JS+CSS)
│   │           ├── index.html                  # Страница входа
│   │           ├── client.html                 # Дашборд клиента
│   │           ├── reception.html              # Дашборд ресепшен
│   │           ├── admin.html                  # Дашборд администратора
│   │           ├── trainer.html                # Дашборд тренера
│   │           ├── css/style.css
│   │           └── js/
│   │               ├── auth.js                 # Логин/регистрация + утилиты
│   │               ├── client.js
│   │               ├── reception.js
│   │               ├── admin.js
│   │               └── trainer.js
│   │
│   └── test/java/com/petgym/
│       └── service/
│           ├── MembershipServiceTest.java      # Unit-тесты сервиса абонементов
│           └── BookingServiceTest.java         # Unit-тесты бронирования
│
├── Dockerfile                                  # Многостадийная сборка Docker-образа
├── docker-compose.yml                          # Запуск app + PostgreSQL в контейнерах
├── .dockerignore
├── build.gradle
├── settings.gradle
├── postman_tests/
│   └── FitnessClub.postman_collection.json    # Коллекция Postman
└── README.md
```

---

## 4. База данных — схема и сущности

### Схема таблиц (ERD)

```
users (id, email, password, first_name, last_name, phone, role, created_at, enabled)
  │
  ├──1:1── clients (user_id, birth_date)
  │
  ├──1:1── trainers (user_id, specialization, bio)
  │
  ├──1:N── purchases (id, client_id→users, type_id→membership_types,
  │                   start_date, end_date, paid_amount, payment_date)
  │
  ├──1:N── training_bookings (id, client_id→users, trainer_id→users,
  │                           start_date_time, end_date_time, status, cancellation_reason,
  │                           created_at, version)
  │
  ├──1:N── workout_programs (id, client_id→users, trainer_id→users, name,
  │                          created_at, updated_at)
  │         └──1:N── workout_exercises (id, program_id→workout_programs,
  │                                     exercise_name, sets, reps, weight,
  │                                     day_number, order_index)
  │
  ├──1:N── visits (id, client_id→users, visit_date, marked_by→users, marked_at)
  │
  └──1:N── notifications (id, user_id→users, message, is_read, created_at)

membership_types (id, name, duration_days, price, is_active)
```

### Описание сущностей

#### `User` — базовый пользователь
Все роли хранятся в одной таблице. Поле `role` — enum-строка.

```java
role: CLIENT | TRAINER | RECEPTION | ADMIN
```

> Почему одна таблица? Это паттерн **Single Table Inheritance** (упрощённый вариант).
> Клиенты и тренеры имеют дополнительные таблицы (`clients`, `trainers`) связанные 1:1.

#### `Client` / `Trainer` — расширения User
Используется аннотация `@MapsId` — `user_id` одновременно является и первичным ключом, и внешним ключом.

```java
@Id @Column(name = "user_id")
private Long userId;

@OneToOne(fetch = FetchType.LAZY)
@MapsId                          // ← говорит JPA: userId = user.id
@JoinColumn(name = "user_id")
private User user;
```

#### `TrainingBooking` — бронирование тренировки
Имеет поле `@Version` — это **оптимистичная блокировка** (Optimistic Locking).
При одновременных попытках создать пересекающееся бронирование одна из транзакций получит
`OptimisticLockingFailureException` вместо записи двойного бронирования.

```java
@Version
private Long version;    // автоматически увеличивается при каждом UPDATE
```

#### `WorkoutProgram` + `WorkoutExercise`
Связь OneToMany с `cascade = CascadeType.ALL, orphanRemoval = true`.
Это значит: при удалении программы все упражнения удаляются автоматически.
При обновлении программы сначала очищается список `exercises`, затем добавляются новые — и Hibernate удалит «осиротевшие» записи.

#### Flyway-миграции

Flyway отслеживает историю миграций в таблице `flyway_schema_history`.
Файлы миграций — SQL-скрипты с именами `V{версия}__{описание}.sql`:

| Файл | Что делает |
|------|-----------|
| `V1__init.sql` | Создаёт все 10 таблиц |
| `V2__data.sql` | Вставляет 4 типа абонементов |

> **Важно**: никогда не изменяй уже применённые миграции — Flyway сверяет их контрольные суммы.
> Для изменений создавай новый файл `V3__...sql`.

---

## 5. Безопасность и JWT

### Как работает аутентификация

```
1. Клиент → POST /api/auth/login (email + password)
2. AuthService → AuthenticationManager.authenticate()  ← Spring Security проверяет пароль
3. JwtTokenProvider.generateToken(userId, role)        ← создаём JWT
4. Ответ: { token, role, userId, ... }

--- Каждый следующий запрос ---
5. Клиент → GET /api/client/bookings
            Header: Authorization: Bearer eyJhbGci...

6. JwtAuthenticationFilter (OncePerRequestFilter):
   a. извлекает токен из заголовка
   b. JwtTokenProvider.validateToken(token) → true/false
   c. если true → достаём userId из claims
   d. UserDetailsServiceImpl.loadUserById(userId)
   e. UsernamePasswordAuthenticationToken → SecurityContextHolder

7. Spring Security видит роль → пропускает/блокирует доступ к эндпоинту
```

### Структура JWT токена

```
Header:  { "alg": "HS384" }
Payload: { "sub": "5",           ← userId как строка
           "role": "CLIENT",
           "iat": 1776706520,    ← issued at (секунды)
           "exp": 1776792920 }   ← expires at (+24 часа)
Signature: HMAC-SHA384(header + "." + payload, SECRET_KEY)
```

> Секретный ключ — 64-символьная hex-строка в `application.properties`.
> В production замени на случайно сгенерированный ключ и храни в переменных окружения.

### Правила доступа (SecurityConfig)

```java
.requestMatchers("/api/auth/**").permitAll()          // регистрация и логин — без токена
.requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
.requestMatchers("/api/client/**").hasRole("CLIENT")  // только CLIENT
.requestMatchers("/api/reception/**").hasRole("RECEPTION")
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/trainer/**").hasRole("TRAINER")
.anyRequest().authenticated()
```

### BCrypt

Пароли хранятся как BCrypt-хэши (10 раундов).
BCrypt — необратимая функция: по хэшу нельзя восстановить пароль.

```
"admin" → $2a$10$bK...длинный хэш...
```

При логине Spring Security сравнивает введённый пароль с хэшем через `BCryptPasswordEncoder.matches()`.

---

## 6. REST API — все эндпоинты

### Формат ошибок

При любой ошибке возвращается:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Описание ошибки",
  "path": "/api/client/bookings",
  "timestamp": "2026-04-20T10:00:00"
}
```

### Auth (`/api/auth`)

| Метод | URL | Тело запроса | Ответ |
|-------|-----|-------------|-------|
| `POST` | `/api/auth/register` | `{email, password, firstName, lastName, phone?}` | `AuthResponse` |
| `POST` | `/api/auth/login` | `{email, password}` | `AuthResponse` |

`AuthResponse`:
```json
{
  "token": "eyJhbG...",
  "role": "CLIENT",
  "userId": 5,
  "firstName": "Пётр",
  "lastName": "Клиентов",
  "email": "client1@fit.com"
}
```

### Client (`/api/client`) — роль CLIENT

| Метод | URL | Описание |
|-------|-----|---------|
| `GET` | `/api/client/memberships/types` | Все активные типы абонементов |
| `GET` | `/api/client/memberships/active` | Мои абонементы (все) |
| `POST` | `/api/client/memberships/buy/{typeId}` | Купить абонемент (startDate = сегодня) |
| `GET` | `/api/client/trainers` | Список всех тренеров |
| `GET` | `/api/client/trainers/{id}/slots?date=YYYY-MM-DD` | Свободные часовые слоты (9–18ч) |
| `POST` | `/api/client/bookings` | Создать бронирование `{trainerId, startDateTime}` |
| `GET` | `/api/client/bookings` | Все мои бронирования |
| `DELETE` | `/api/client/bookings/{id}` | Отменить (только за ≥2 часа до начала) |
| `GET` | `/api/client/workout-program` | Моя программа тренировок |
| `GET` | `/api/client/notifications` | Непрочитанные уведомления |
| `POST` | `/api/client/notifications/read` | Пометить все как прочитанные |

### Reception (`/api/reception`) — роль RECEPTION

| Метод | URL | Описание |
|-------|-----|---------|
| `GET` | `/api/reception/clients?query=...` | Поиск клиентов по email/телефону |
| `POST` | `/api/reception/clients` | Создать нового клиента |
| `POST` | `/api/reception/memberships` | Оформить абонемент `{clientId, typeId, startDate?}` |
| `GET` | `/api/reception/memberships/active` | Все активные абонементы |
| `POST` | `/api/reception/visits` | Отметить посещение `{clientId}` |
| `GET` | `/api/reception/visits/today` | Посещения сегодня |

### Admin (`/api/admin`) — роль ADMIN

| Метод | URL | Описание |
|-------|-----|---------|
| `GET` | `/api/admin/membership-types` | Все типы абонементов |
| `POST` | `/api/admin/membership-types` | Создать тип `{name, durationDays, price}` |
| `PUT` | `/api/admin/membership-types/{id}` | Обновить тип |
| `DELETE` | `/api/admin/membership-types/{id}` | Деактивировать тип (soft delete) |
| `GET` | `/api/admin/reports/revenue?from=...&to=...` | Финансовый отчёт |
| `GET` | `/api/admin/users` | Список сотрудников |
| `POST` | `/api/admin/users` | Создать сотрудника `{email, password, role, ...}` |

`RevenueReportDto`:
```json
{
  "totalRevenue": 28500.00,
  "totalMembershipsSold": 12,
  "totalTrainingsCount": 47,
  "membershipsByType": {
    "1 месяц — безлимит": 7,
    "3 месяца — безлимит": 5
  }
}
```

### Trainer (`/api/trainer`) — роль TRAINER

| Метод | URL | Описание |
|-------|-----|---------|
| `GET` | `/api/trainer/bookings` | Предстоящие тренировки |
| `PUT` | `/api/trainer/bookings/{id}/confirm` | Подтвердить бронирование |
| `PUT` | `/api/trainer/bookings/{id}/cancel` | Отменить `{reason}` (уведомляет клиента) |
| `GET` | `/api/trainer/clients` | Клиенты, бронировавшие тренировки у меня |
| `GET` | `/api/trainer/clients/{id}/workout-program` | Программа клиента |
| `POST` | `/api/trainer/clients/{id}/workout-program` | Создать программу `{name, exercises[]}` |
| `PUT` | `/api/trainer/clients/{id}/workout-program/{pid}` | Обновить программу |
| `GET` | `/api/trainer/programs` | Все мои программы |
| `GET` | `/api/trainer/notifications` | Уведомления |
| `POST` | `/api/trainer/notifications/read` | Прочитать все |

---

## 7. Бизнес-логика (сервисный слой)

### MembershipService — абонементы

**`hasActiveMembership(clientId, date)`** — ключевой метод:
```sql
SELECT COUNT(*) > 0 FROM purchases
WHERE client_id = :clientId
  AND start_date <= :date
  AND end_date >= :date
```
Используется в `BookingService`, `VisitService`, `WorkoutService` — везде, где нужно проверить право доступа.

**Покупка абонемента**: `endDate = startDate + durationDays`.
Параллельные абонементы разрешены — клиент может купить новый, не дождавшись окончания текущего.

### BookingService — бронирование тренировок

Алгоритм создания брони (все проверки до записи в БД):

```
1. startDateTime > now()                     → BusinessException
2. startDateTime <= now() + 7 дней           → BusinessException
3. hasActiveMembership(clientId, date)       → MembershipExpiredException
4. count(будущих броней у этого тренера) < 2 → BookingConflictException
5. findConflictingBookings(trainerId, ...)   → BookingConflictException (с пессимистичной блокировкой)
6. save(booking) + уведомление тренеру
```

**Пессимистичная блокировка** при проверке конфликтов:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
List<TrainingBooking> findConflictingBookings(...);
```
Это гарантирует, что два одновременных запроса на один слот не пройдут оба — один из них будет ждать снятия блокировки.

**Отмена клиентом**:
```
now() < startDateTime - 2 часа → можно отменить
now() ≥ startDateTime - 2 часа → InvalidCancellationException
```

### WorkoutService — программы тренировок

При создании программы проверяется `hasActiveMembership` — нельзя создать программу для клиента без абонемента.

Упражнения хранятся с полями `dayNumber` (день программы) и `orderIndex` (порядок в дне).
При обновлении программы — orphanRemoval очищает старые упражнения и вставляет новые.

### NotificationService — уведомления

Уведомления создаются автоматически в `BookingService`:
- При создании брони → тренеру: "Новая тренировка"
- При отмене клиентом → тренеру: "Клиент отменил"
- При отмене тренером → клиенту: "Тренер отменил"

### GlobalExceptionHandler

Все `RuntimeException` из сервисов перехватываются `@RestControllerAdvice` и превращаются в HTTP-ответы:

```
ResourceNotFoundException       → 404 Not Found
MembershipExpiredException      → 403 Forbidden
BookingConflictException         → 409 Conflict
InvalidCancellationException     → 400 Bad Request
BusinessException                → 400 Bad Request
BadCredentialsException          → 401 Unauthorized
AccessDeniedException            → 403 Forbidden
MethodArgumentNotValidException  → 400 (ошибки @Valid валидации)
Exception (всё остальное)        → 500 Internal Server Error
```

---

## 8. Фоновые задачи

Файл: `ScheduledTasks.java`

Для работы нужна аннотация `@EnableScheduling` на главном классе.

| Задача | Cron | Что делает |
|--------|------|-----------|
| Очистка старых уведомлений | `0 5 0 * * *` (00:05 каждый день) | Удаляет уведомления старше 30 дней |
| Напоминания об истечении абонемента | `0 0 8 * * *` (08:00 каждый день) | Находит абонементы, истекающие через 1–3 дня, отправляет уведомление |

**Формат cron в Spring**: `секунды минуты часы дни месяцы дни_недели`

---

## 9. Фронтенд

Фронтенд — **статические HTML-файлы** в `src/main/resources/static/`.
Spring Boot раздаёт их как статику — никакого SSR, никакого Thymeleaf.

### Принцип работы

```
1. Пользователь открывает / → Spring Boot отдаёт index.html
2. JS делает POST /api/auth/login → получает JWT
3. JWT сохраняется в localStorage
4. Редирект на нужный дашборд (client.html и т.д.)
5. Все последующие запросы идут с заголовком Authorization: Bearer <token>
6. Если token отсутствует или не соответствует роли — редирект на index.html
```

### auth.js — общие утилиты

```javascript
apiCall(method, path, body, token)  // базовый fetch с обработкой ошибок
checkAuth(expectedRole)              // проверяет localStorage, редиректит если нет токена
logout()                             // очищает localStorage
getToken() / getUserId()             // геттеры из localStorage
get(path) / post(path, body)         // обёртки apiCall с автоподставкой токена
fmtDt(datetime) / fmtDate(date)     // форматирование дат для русской локали
showErr(elementId, msg)             // показывает ошибку в alert-блоке
showOk(elementId, msg)              // показывает успех
```

### Страницы

| Файл | Роль | Функциональность |
|------|------|-----------------|
| `index.html` | — | Форма входа + форма регистрации |
| `client.html` | CLIENT | Вкладки: абонементы, тренеры+бронирование, тренировки, программа, уведомления |
| `reception.html` | RECEPTION | Вкладки: поиск клиентов, новый клиент, оформление абонемента, посещения, активные абонементы |
| `admin.html` | ADMIN | Вкладки: типы абонементов (CRUD), отчёты, сотрудники |
| `trainer.html` | TRAINER | Вкладки: расписание, клиенты, редактор программ тренировок, уведомления |

---

## 10. Запуск проекта

### Требования

- **Java 17+** (проект использует синтаксис Java 17)
- **PostgreSQL** 14+ (поддерживается до 18.x)
- **Gradle** (есть gradlew — скачивать отдельно не нужно)

### Шаг 1. Создай базу данных

```sql
-- Подключись к PostgreSQL и выполни:
CREATE DATABASE petgym;
```

### Шаг 2. Настрой подключение

`src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/petgym
spring.datasource.username=postgres
spring.datasource.password=1234
server.port=8091
```

### Шаг 3. Запусти приложение

```bash
# Windows
gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

При первом запуске Flyway автоматически применит `V1__init.sql` и `V2__data.sql`.
`DataInitializer` создаст 6 тестовых пользователей.

### Шаг 4. Открой в браузере

| URL | Описание |
|-----|---------|
| http://localhost:8091 | Страница входа |
| http://localhost:8091/swagger-ui.html | Swagger UI (документация API) |
| http://localhost:8091/client.html | Дашборд клиента |
| http://localhost:8091/admin.html | Дашборд администратора |

---

## 11. Docker и Docker Compose

### Запуск одной командой (рекомендуется для демонстрации)

```bash
docker compose up --build
```

Это создаст и запустит два контейнера:
- `petgym-db` — PostgreSQL 17 (данные хранятся в именованном volume)
- `petgym-app` — Spring Boot приложение

Приложение доступно на **http://localhost:8091**

> `petgym-app` ждёт готовности `petgym-db` через `depends_on + condition: service_healthy`

### Остановить и удалить контейнеры

```bash
docker compose down          # остановить, данные БД сохранятся
docker compose down -v       # остановить + удалить volume с данными БД
```

### Пересобрать образ после изменений кода

```bash
docker compose up --build    # пересборка образа + перезапуск
```

### Dockerfile — многостадийная сборка

```
Stage 1: gradle:8.7-jdk21 AS builder
  ├── Копируем build.gradle + gradle/       ← слой кешируется
  ├── gradle dependencies                   ← загружаем зависимости (кеш)
  ├── Копируем src/                         ← исходники
  └── gradle bootJar → build/libs/*.jar

Stage 2: eclipse-temurin:21-jre-alpine
  ├── Минимальный JRE образ (~100 MB vs ~500 MB с JDK)
  ├── Создаём непривилегированного пользователя petgym
  ├── COPY --from=builder .../app.jar
  └── ENTRYPOINT java $JAVA_OPTS -jar app.jar
```

> **Многостадийная сборка** важна: если бы мы копировали JAR из локального `build/`,
> каждый раз при изменении кода нужна локальная сборка перед `docker build`.
> Сейчас Docker сам собирает JAR внутри контейнера.

### Порт PostgreSQL в docker-compose

```yaml
ports:
  - "5433:5432"   # 5433 на хосте → 5432 в контейнере
```

Маппинг на 5433 нужен, чтобы не конфликтовать с локальным PostgreSQL на порту 5432.
Если хочешь подключиться из DBeaver/psql к PostgreSQL в контейнере — используй порт 5433.

---

## 12. Тестирование через Postman

### Импорт коллекции

1. Открой Postman
2. **Import** → выбери `postman_tests/FitnessClub.postman_collection.json`
3. Коллекция появится с разделами: Auth, Client, Reception, Admin, Trainer, Security Tests

### Переменные коллекции

Переменные хранятся на уровне коллекции (вкладка Variables):

| Переменная | Значение по умолчанию | Как устанавливается |
|-----------|----------------------|---------------------|
| `baseUrl` | `http://localhost:8091` | вручную |
| `adminToken` | пусто | после запроса "Login Admin" |
| `receptionToken` | пусто | после "Login Reception" |
| `trainer1Token` | пусто | после "Login Trainer1" |
| `clientToken` | пусто | после "Login Client1" |
| `trainerId` | пусто | после "Get Trainers" (берёт первого) |
| `bookingId` | пусто | после "Create Booking" |
| `programId` | пусто | после "Create Workout Program" |
| `clientId` | пусто | после "Login Client1" |

### Рекомендуемый порядок тестирования

```
1. Auth → Login Admin       (устанавливает adminToken)
2. Auth → Login Reception   (устанавливает receptionToken)
3. Auth → Login Trainer1    (устанавливает trainer1Token)
4. Auth → Login Client1     (устанавливает clientToken + clientId)
5. Client → Get Trainers    (устанавливает trainerId)
6. Client → Buy Membership  (нужен активный абонемент для бронирования)
7. Client → Create Booking  (устанавливает bookingId)
8. Trainer → Get My Schedule      (видим бронь)
9. Trainer → Create Workout Program  (устанавливает programId)
10. Client → Get My Workout Program  (видим созданную программу)
```

---

## 13. Тестовые аккаунты

Создаются автоматически классом `DataInitializer` при первом запуске.

| Email | Пароль | Роль | Дашборд |
|-------|--------|------|---------|
| `admin@fit.com` | `admin` | ADMIN (Бухгалтерия) | `/admin.html` |
| `reception@fit.com` | `reception` | RECEPTION | `/reception.html` |
| `trainer1@fit.com` | `trainer1` | TRAINER (силовые) | `/trainer.html` |
| `trainer2@fit.com` | `trainer2` | TRAINER (йога) | `/trainer.html` |
| `client1@fit.com` | `client1` | CLIENT | `/client.html` |
| `client2@fit.com` | `client2` | CLIENT | `/client.html` |

---

## 14. Частые ошибки и их решения

### `Unsupported Database: PostgreSQL 18.0`

**Причина**: Старая версия Flyway не знает о PostgreSQL 18.

**Решение**: Добавить модуль `flyway-database-postgresql` в `build.gradle`:
```groovy
ext['flyway.version'] = '10.21.0'
implementation 'org.flywaydb:flyway-database-postgresql'
```

---

### `Port 8091 already in use`

**Причина**: Другой процесс занимает порт.

**Решение (Windows)**:
```bash
netstat -ano | findstr :8091
taskkill /F /PID <PID>
```

Или измени порт в `application.properties`:
```properties
server.port=8092
```

---

### `Schema-validation: missing column [version] in table [training_bookings]`

**Причина**: В сущности `TrainingBooking` есть `@Version private Long version`,
но в миграции `V1__init.sql` этот столбец отсутствует.

**Решение**: Убедись, что `V1__init.sql` содержит:
```sql
version BIGINT DEFAULT 0
```
в таблице `training_bookings`.

---

### `ExceptionInInitializerError` при сборке через Gradle 9.x

**Причина**: Несовместимость плагинов с Gradle 9.

**Решение**: В `build.gradle` используй:
```groovy
id 'org.springframework.boot' version '3.4.4'
id 'io.spring.dependency-management' version '1.1.7'
```

---

### `Flyway upgrade recommended: PostgreSQL 18 is newer...`

Это **предупреждение**, не ошибка. Приложение работает нормально.
Flyway просто сообщает, что версия PostgreSQL новее максимально протестированной.

---

### JWT 401 Unauthorized

Возможные причины:
1. Токен истёк (срок жизни 24 часа по умолчанию)
2. Неверный заголовок — должен быть `Authorization: Bearer <token>` (с пробелом)
3. Обращение к чужому роли (`CLIENT` → `/api/admin/...` вернёт 403, не 401)

---

### Тест `BookingConflictException` — тренер уже занят

```bash
# Попробуй забронировать одно и то же время дважды:
POST /api/client/bookings
{ "trainerId": 3, "startDateTime": "2026-04-25T10:00:00" }

# Первый запрос → 200 OK
# Второй запрос → 409 Conflict: "Тренер уже занят в это время"
```

---

## Структура JWT в деталях

JWT состоит из трёх частей, разделённых точкой: `HEADER.PAYLOAD.SIGNATURE`

```
eyJhbGciOiJIUzM4NCJ9           ← BASE64({"alg":"HS384"})
.
eyJzdWIiOiI1IiwicmFsZSI6...   ← BASE64({"sub":"5","role":"CLIENT",...})
.
P9kuCBci1Ys65QPwnz5f...        ← HMAC-SHA384 подпись
```

**Декодировать** любой токен (без проверки подписи) можно на https://jwt.io

---

*Проект создан как учебный пример Spring Boot. Для production-использования необходимо:*
*замена секретного ключа JWT, HTTPS, rate limiting, полноценное логирование, мониторинг.*
