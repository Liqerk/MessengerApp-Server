
<div align="center">

# 🚀 Messenger Server

### _Production-ready Messaging Backend with Ktor, WebSockets, and PostgreSQL_

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Ktor](https://img.shields.io/badge/Ktor-2.3.0-087CFA.svg?style=for-the-badge&logo=ktor&logoColor=white)](https://ktor.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1.svg?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

[![Status](https://img.shields.io/badge/Status-Production_Ready-00C853.svg?style=flat-square)]()
[![License](https://img.shields.io/badge/License-MIT-F5A623.svg?style=flat-square)](LICENSE)

> **Современный сервер мессенджера** на Kotlin + Ktor с real-time WebSocket, JWT-аутентификацией и полной Docker-поддержкой.

</div>

---

## 🎯 **О проекте**

**Messenger Server** — это бэкенд для мессенджера, разработанный с нуля на Kotlin с использованием фреймворка Ktor. Сервер обеспечивает real-time обмен сообщениями через WebSocket, надежное хранение данных в PostgreSQL и полную готовность к production-эксплуатации (Docker, health checks, CORS).

### **Ключевые возможности**
- ✅ **Real-time сообщения** через WebSocket с авто-реконнектом
- ✅ **JWT аутентификация** с BCrypt-хешированием паролей
- ✅ **PostgreSQL + Exposed ORM** для надежного хранения
- ✅ **Загрузка файлов** (изображения, аудио, видео)
- ✅ **Статусы "онлайн/оффлайн"** и "печатает..."
- ✅ **Полное удаление сообщений** и чатов
- ✅ **Docker Compose** для мгновенного развертывания

---

## 🏗️ **Архитектура**

```
┌─────────────────────────────────────────┐
│         Clients (Android/Web)           │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│              Ktor Server                │
│  ┌──────────┬──────────┬────────────┐  │
│  │   REST   │ WebSocket │   Upload   │  │
│  │   API    │  (/ws)    │   (/file)  │  │
│  └──────────┴──────────┴────────────┘  │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│           Service Layer                 │
│  Auth │ Message │ Chat │ User │ WS      │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│      Exposed ORM + PostgreSQL           │
│    users │ messages │ pinned_chats      │
└─────────────────────────────────────────┘
```

### **Примененные паттерны**
- **Repository Pattern** — абстракция доступа к данным
- **Service Layer** — бизнес-логика изолирована от API
- **Dependency Injection** — ручное внедрение через конструкторы
- **DTO → Domain → Entity** — четкое разделение слоев

---

## ✨ **API Endpoints**

### **🔐 Аутентификация**

| Метод | Endpoint | Описание |
|-------|----------|----------|
| `POST` | `/auth/register` | Регистрация нового пользователя |
| `POST` | `/auth/login` | Вход и получение JWT-токена |

### **💬 Сообщения**

| Метод | Endpoint | Описание |
|-------|----------|----------|
| `GET` | `/messages?user1=&user2=` | Получить историю переписки |
| `POST` | `/messages` | Отправить сообщение (REST fallback) |
| `DELETE` | `/messages/{id}` | Удалить сообщение (полностью из БД) |

### **👥 Пользователи и чаты**

| Метод | Endpoint | Описание |
|-------|----------|----------|
| `GET` | `/users/search?q=` | Поиск пользователей |
| `GET` | `/chats?user=` | Получить список чатов |
| `POST` | `/chats/{partner}/pin` | Закрепить/открепить чат |

### **📎 Файлы**

| Метод | Endpoint | Описание |
|-------|----------|----------|
| `POST` | `/upload` | Загрузить файл (изображение/аудио/видео) |

---

## 🔌 **WebSocket Protocol**

### **Подключение**
```bash
ws://localhost:8080/ws?token=YOUR_JWT_TOKEN
```

### **Формат сообщений**

**Отправка текста:**
```json
{
  "type": "message",
  "payload": {
    "receiver": "username",
    "text": "Hello, world!",
    "clientMessageId": "uuid-1234-5678"
  }
}
```

**Индикатор набора текста:**
```json
{
  "type": "typing",
  "payload": {
    "receiver": "username",
    "isTyping": true
  }
}
```

**Удаление сообщения:**
```json
{
  "type": "delete",
  "payload": {
    "clientMessageId": "uuid-1234-5678"
  }
}
```

**Удаление чата:**
```json
{
  "type": "delete_chat",
  "payload": {
    "chatWith": "username"
  }
}
```

### **События от сервера**
```json
// Новое сообщение
{ "type": "message", "payload": { ... } }

// Статус "онлайн"
{ "type": "online", "payload": { "login": "user", "isOnline": true } }

// Событие "печатает..."
{ "type": "typing", "payload": { "sender": "user", "isTyping": true } }
```

---

## 🚀 **Быстрый старт**

### **Требования**
- Docker & Docker Compose **или**
- JDK 17+, PostgreSQL 15+

### **С Docker (рекомендуется)**

```bash
# 1. Клонирование репозитория
git clone https://github.com/Liqerk/MessengerApp-Server.git
cd MessengerApp-Server

# 2. Настройка окружения
cp .env.example .env
# Обязательно установите свой JWT_SECRET в .env файле!

# 3. Запуск контейнеров
docker-compose up -d

# 4. Проверка работы
curl http://localhost:8080/health
# Ответ: {"status":"ok","timestamp":1234567890}
```

### **Локальный запуск (без Docker)**

```bash
# Настройка БД
createdb messenger
psql -c "CREATE USER messenger WITH PASSWORD 'your_password';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE messenger TO messenger;"

# Экспорт переменных окружения
export DB_URL="jdbc:postgresql://localhost:5432/messenger"
export DB_USER="messenger"
export DB_PASSWORD="your_password"
export JWT_SECRET="your-super-secret-key-min-32-chars"

# Запуск
./gradlew run
```

---

## 🛠️ **Технический стек**

| Компонент | Технология | Версия | Назначение |
|-----------|------------|--------|------------|
| **Язык** | Kotlin | 2.0.0 | Основной язык сервера |
| **Фреймворк** | Ktor | 2.3.0 | Веб-фреймворк (REST + WebSocket) |
| **ORM** | Exposed | 0.48.1 | Типобезопасные SQL-запросы |
| **БД** | PostgreSQL | 15+ | Реляционная база данных |
| **Auth** | JWT + BCrypt | - | Аутентификация и хеширование |
| **Сборка** | Gradle (Kotlin DSL) | 8.5 | Управление зависимостями |
| **Контейнеризация** | Docker | 24+ | Развертывание |

---

## 📁 **Структура проекта**

```
messenger-server/
├── src/main/kotlin/com/messenger/
│   ├── config/               # Конфигурация (JWT, AppConfig)
│   ├── database/             # Exposed таблицы (Users, Messages, PinnedChats)
│   ├── domain/               # Бизнес-логика
│   │   ├── model/            # Domain-модели (User, Message, Chat)
│   │   ├── repository/       # Интерфейсы репозиториев
│   │   └── service/          # Сервисы (Auth, Message, Chat, WebSocket)
│   ├── infrastructure/       # Реализации
│   │   ├── database/         # Exposed-репозитории
│   │   └── websocket/        # ConnectionManager
│   └── presentation/         # API слой
│       ├── dto/              # Data Transfer Objects
│       └── route/            # Ktor-маршруты (auth, messages, websocket)
├── .env.example              # Пример переменных окружения
├── docker-compose.yml        # Оркестрация (сервер + PostgreSQL)
├── Dockerfile                # Многостадийная сборка
└── build.gradle.kts          # Конфигурация Gradle
```

---

## 🔧 **Конфигурация (.env)**

```bash
# Сервер
SERVER_PORT=8080
SERVER_HOST=0.0.0.0

# База данных
DB_URL=jdbc:postgresql://postgres:5432/messenger
DB_USER=messenger
DB_PASSWORD=your_secure_password

# JWT (обязательно измените!)
JWT_SECRET=your-super-secret-key-with-at-least-32-characters
JWT_ISSUER=messenger-server
JWT_AUDIENCE=messenger-client
JWT_EXPIRATION_DAYS=7
```

---

## 🧪 **Тестирование API**

```bash
# Health check
curl http://localhost:8080/health

# Регистрация
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"login":"john","email":"john@example.com","password":"secret"}'

# Логин
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"john","password":"secret"}'

# Получение сообщений (с токеном)
curl http://localhost:8080/messages?user1=john&user2=jane \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 📊 **Мониторинг**

```bash
# Просмотр логов
docker-compose logs -f messenger-server

# Статистика WebSocket (из логов)
docker-compose logs messenger-server | grep "WebSocket connected"

# Прямой запрос к БД
docker-compose exec postgres psql -U messenger -d messenger
```

---

## 🤝 **Связь с клиентом**

Проект является серверной частью мессенджера. Вебсокеты совместимы с клиентом: **[MessengerApp для Android](https://github.com/Liqerk/MessengerApp)**.

---

## 🔮 **Планы развития**

| Задача | Приоритет | Статус |
|--------|-----------|--------|
| Unit-тесты для сервисов | Высокий | ⚠️ Частично |
| Rate limiting | Средний | ❌ Не начат |
| Поддержка видеозвонков | Низкий | ❌ Не начат |

---

## 📄 **Лицензия**

MIT © [Liqerk](https://github.com/Liqerk)

---

<div align="center">

### **🌟 Готов к работе в production! 🌟**

**[→ Открыть клиент](https://github.com/Liqerk/MessengerApp) · [Сообщить о проблеме](https://github.com/Liqerk/MessengerApp-Server/issues)**

*Сервер успешно протестирован с Android-клиентом, поддерживает WebSocket-реконнект, полное удаление данных и офлайн-синхронизацию*

[![Telegram](https://img.shields.io/badge/Telegram-@Liqerk-0088cc?logo=telegram&logoColor=white)](https://t.me/Liqerk)

</div>

