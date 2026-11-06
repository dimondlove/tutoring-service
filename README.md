# Tutoring Service

Веб-приложение для организации занятий с репетиторами. Проект включает REST API на Spring Boot, PostgreSQL и веб-интерфейс на HTML/CSS, который раздаётся через Nginx.

## Содержание

- [Возможности](#возможности)
- [Технологии](#технологии)
- [Требования](#требования)
- [Запуск проекта через Docker Compose](#запуск-проекта-через-docker-compose)
- [Проверка работы](#проверка-работы)
- [Демо-аккаунты](#демо-аккаунты)
- [Локальный запуск backend](#локальный-запуск-backend)
- [Запуск тестов](#запуск-тестов)
- [Остановка проекта](#остановка-проекта)
- [Структура проекта](#структура-проекта)

## Возможности

- регистрация и авторизация пользователей;
- роли ученика, репетитора и администратора;
- просмотр репетиторов и предметов;
- расписание и доступные временные слоты;
- создание и управление бронированиями занятий;
- административное управление пользователями, репетиторами, предметами и расписанием;
- API-документация Swagger UI.

## Технологии

- **Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway, Gradle;
- **База данных:** PostgreSQL 16;
- **Frontend:** HTML, CSS;
- **Веб-сервер frontend:** Nginx;
- **Запуск:** Docker Compose.

## Требования

Для запуска всего проекта:

- Docker Desktop или Docker Engine с поддержкой Docker Compose;
- Git (если проект клонируется из репозитория).

Для запуска backend без Docker дополнительно нужны JDK 21 и PostgreSQL.

## Запуск проекта через Docker Compose

### 1. Получить проект

Клонируйте репозиторий или откройте папку проекта:

```bash
git clone <URL-репозитория>
cd tutoring-service
```

Если проект уже скачан, перейдите в его корневую папку — туда, где находится `docker-compose.yml`.

### 2. Подготовить переменные окружения

Создайте файл `.env` на основе примера:

**Windows PowerShell:**

```powershell
Copy-Item .env.example .env
```

**Linux / macOS:**

```bash
cp .env.example .env
```

В `.env` заданы параметры подключения к PostgreSQL и секрет JWT. Для локального запуска можно использовать значения из примера. Не используйте демонстрационные значения в production и не публикуйте `.env`.

### 3. Запустить сервисы

Из корневой папки проекта выполните:

```bash
docker compose up --build -d
```

Команда запустит:

| Сервис | Адрес |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| Health check | http://localhost:8080/actuator/health |
| PostgreSQL | `localhost:5432` (доступен локально) |

Backend запускается после того, как PostgreSQL станет доступен. При первом старте Flyway создаёт структуру базы данных. В профиле `demo` также добавляются демонстрационные данные, если база ещё пуста.

### 4. Проверить состояние сервисов

```bash
docker compose ps
```

Посмотреть логи backend:

```bash
docker compose logs -f backend
```

Логи всех сервисов:

```bash
docker compose logs -f
```

## Проверка работы

1. Откройте frontend: http://localhost:3000.
2. Проверьте доступность API: http://localhost:8080/actuator/health. При успешном запуске ответ содержит статус `UP`.
3. Откройте Swagger UI: http://localhost:8080/swagger-ui/index.html — там можно просматривать и вызывать API-методы.
4. Для проверки авторизации используйте один из демонстрационных аккаунтов ниже.

## Демо-аккаунты

При запуске с профилем `demo` тестовые пользователи и данные создаются только в пустой базе данных.

| Роль | Email | Пароль |
|---|---|---|
| Администратор | `admin@demo.com` | `Admin123!` |
| Репетитор | `tutor1@demo.com` | `Tutor123!` |
| Репетитор | `tutor2@demo.com` | `Tutor123!` |
| Ученик | `student@demo.com` | `Student123!` |
| Ученик | `student2@demo.com` | `Student123!` |

Если база уже содержит пользователей, демо-данные повторно не добавляются. Чтобы начать с чистой демо-базы, можно удалить Docker volume с данными PostgreSQL — **это удалит сохранённые данные**:

```bash
docker compose down -v
docker compose up --build -d
```

## Локальный запуск backend

Этот вариант подходит для разработки, если PostgreSQL запущен отдельно.

1. Установите JDK 21 и запустите PostgreSQL.
2. Создайте базу данных и пользователя. Для значений по умолчанию из `application.yml` подойдут база `tutoring`, пользователь `tutoring`, пароль `tutoring`.
3. Перейдите в папку backend:

   **Windows PowerShell:**

   ```powershell
   cd backend\tutoring-backend
   .\gradlew.bat bootRun
   ```

   **Linux / macOS:**

   ```bash
   cd backend/tutoring-backend
   ./gradlew bootRun
   ```

Чтобы включить демо-данные при локальном запуске, активируйте профиль `demo`.

**PowerShell:**

```powershell
$env:SPRING_PROFILES_ACTIVE = "demo"
.\gradlew.bat bootRun
```

**Linux / macOS:**

```bash
SPRING_PROFILES_ACTIVE=demo ./gradlew bootRun
```

При необходимости параметры базы и JWT можно переопределить переменными окружения `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` и `JWT_SECRET`.

Frontend при Docker-запуске доступен через Nginx на порту `3000`. При локальном запуске backend отдельно откройте HTML-файлы из папки `frontend` через локальный веб-сервер, чтобы избежать проблем с браузерными ограничениями.

## Запуск тестов

Тесты backend запускаются Gradle. Интеграционные тесты используют Testcontainers, поэтому для них должен быть доступен Docker.

**Windows PowerShell:**

```powershell
cd backend\tutoring-backend
.\gradlew.bat test
```

**Linux / macOS:**

```bash
cd backend/tutoring-backend
./gradlew test
```

Для проверки API через PowerShell-скрипт из каталога `scripts` (если он присутствует в вашей рабочей копии):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api-check.ps1
```

Скрипт по умолчанию проверяет API на `http://localhost:8080`. При необходимости можно передать адрес backend и число параллельных запросов:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api-check.ps1 -Base "http://localhost:8080" -Racers 8
```

## Остановка проекта

Остановить контейнеры, сохранив данные базы:

```bash
docker compose down
```

Остановить контейнеры и удалить volume базы данных (**данные будут потеряны**):

```bash
docker compose down -v
```

## Структура проекта

```text
tutoring-service/
├── backend/
│   └── tutoring-backend/   # Spring Boot REST API и тесты
├── frontend/               # HTML/CSS интерфейс
├── docker-compose.yml      # PostgreSQL, backend и frontend
├── .env.example            # пример настроек окружения
└── README.md
```
