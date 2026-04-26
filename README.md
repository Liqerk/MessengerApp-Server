# 🚀 Messenger Server (Ktor + PostgreSQL)

> Real-time мессенджер на Kotlin + Ktor + WebSockets + PostgreSQL



## ✨ Возможности

- ✅ **JWT аутентификация** с хешированием паролей через BCrypt
- ✅ **Real-time сообщения** через WebSockets
- ✅ **PostgreSQL** база данных с Exposed ORM
- ✅ **Загрузка файлов** (изображения, аудио, видео)
- ✅ **Отслеживание статуса** (онлайн/оффлайн)
- ✅ **Удаление сообщений** (полное удаление из БД)
- ✅ **Управление чатами** с закреплением
- ✅ **Docker поддержка** с docker-compose

## 🚀 Быстрый старт

### С Docker (рекомендуется)

```bash
# Клонируйте репозиторий
git clone https://github.com/yourusername/messenger-server.git
cd messenger-server

# Создайте .env файл
cp .env.example .env
nano .env  # Установите JWT_SECRET!

# Запуск
docker-compose up -d

# Проверка
curl http://localhost:8080/health
