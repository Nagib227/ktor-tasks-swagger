# Сервер задач на Ktor со Swagger

CRUD задач, регистрация и JWT. Ошибки собирает плагин `StatusPages`, каждый запрос пишет плагин `CallLogging`. Спецификация OpenAPI открывается в Swagger UI.

Данные хранятся в памяти и сбрасываются при перезапуске. При старте есть три задачи, пользователей нет.

## Требования

- JDK 17 или новее
- Интернет при первом запуске: Gradle скачает зависимости. Страница Swagger UI также подгружает скрипты с unpkg.com

## Запуск

В каталоге проекта:

```bat
gradlew.bat run
```

На Linux и macOS:

```bash
./gradlew run
```

Сервер слушает [http://localhost:8080](http://localhost:8080). Другой порт задаётся переменной `PORT`. Остановка: `Ctrl+C`.

В консоли на каждый запрос есть строка вида `GET /tasks -> 200 (3 ms)`.

## Swagger и OpenAPI

| Адрес | Что это |
| --- | --- |
| [http://localhost:8080/swagger](http://localhost:8080/swagger) | Swagger UI |
| [http://localhost:8080/swagger/documentation.yaml](http://localhost:8080/swagger/documentation.yaml) | Файл OpenAPI |
| [http://localhost:8080/openapi](http://localhost:8080/openapi) | HTML-документация по той же спецификации |

В Swagger UI кнопка Authorize подставляет `Authorization: Bearer <token>`. Токен берётся из `POST /login`.

Спецификация описывает `/`, `/tasks`, `/tasks/{id}`, `/register` и `/login`, отдельные схемы создания и обновления задачи, коды 200, 201, 204, 400, 401, 404 и 409.

## Тесты

Запуск всех тестов:

```bat
gradlew.bat test
```

На Linux и macOS:

```bash
./gradlew test
```

Успешный прогон заканчивается строкой `BUILD SUCCESSFUL`.

Отчёт: `build/reports/tests/test/index.html`.

Тесты проверяют:

- список, получение по id, создание, обновление и удаление задач
- регистрацию, BCrypt-хэш пароля и выдачу JWT
- отказ `401` без токена, с битым, просроченным и чужим токеном
- JSON-ошибки `400`, `401`, `404` и `409` через `StatusPages`
- что Swagger UI открывается, а OpenAPI содержит все маршруты
- что `CallLogging` пишет метод, путь и код ответа

## Маршруты

| Метод | Путь | Токен | Успех |
| --- | --- | --- | --- |
| POST | `/register` | не нужен | 201 |
| POST | `/login` | не нужен | 200 и JWT |
| GET | `/tasks` | не нужен | 200 |
| GET | `/tasks/{id}` | не нужен | 200 |
| POST | `/tasks` | `Authorization: Bearer` | 201 |
| PUT | `/tasks/{id}` | `Authorization: Bearer` | 200 |
| DELETE | `/tasks/{id}` | `Authorization: Bearer` | 204 |

### Регистрация и вход

```json
{ "login": "student", "password": "secret12" }
```

Логин: 3–32 символа, латиница, цифры, `_` и `-`. Пароль: от 6 до 72 символов, в хранилище только BCrypt-хэш. Повтор логина — `409`. Неверный пароль — `401`.

В токене есть срок действия 1 час и claim `userId`.

### Задачи

Создание, без `id`:

```json
{ "title": "Проверить API", "done": false, "priority": "LOW" }
```

`done` по умолчанию `false`, `priority` по умолчанию `MEDIUM`. Обновление передаёт `title`, `done` и `priority`.

## Коды ответов

| Код | Когда |
| --- | --- |
| 200 | Список, одна задача, вход, обновление |
| 201 | Пользователь или задача созданы |
| 204 | Задача удалена |
| 400 | Пустые поля, битый JSON, неверный `id` |
| 401 | Неверный пароль или недействительный токен |
| 404 | Нет задачи или маршрута |
| 409 | Логин или название уже заняты |
| 500 | Непредвиденная ошибка, без стека в ответе |

Тело ошибки:

```json
{ "error": "not_found", "message": "Задача с id=99 не найдена" }
```

## Примеры

Подставьте свой токен вместо `TOKEN`.

PowerShell:

```powershell
curl.exe -X POST http://localhost:8080/register -H "Content-Type: application/json" -d '{"login":"student","password":"secret12"}'
curl.exe -X POST http://localhost:8080/login -H "Content-Type: application/json" -d '{"login":"student","password":"secret12"}'
curl.exe http://localhost:8080/tasks
curl.exe http://localhost:8080/tasks/1
curl.exe -X POST http://localhost:8080/tasks -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d '{"title":"Проверить API","priority":"LOW"}'
curl.exe -X PUT http://localhost:8080/tasks/4 -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d '{"title":"Готово","done":true,"priority":"HIGH"}'
curl.exe -X DELETE http://localhost:8080/tasks/4 -H "Authorization: Bearer TOKEN"
```

Linux и macOS — те же запросы через `curl` без `.exe`.

## Стартовые задачи

| id | Название | done | priority |
| --- | --- | --- | --- |
| 1 | Изучить маршруты Ktor | false | HIGH |
| 2 | Подключить JSON-сериализацию | true | MEDIUM |
| 3 | Написать README | false | LOW |

## Структура

```text
ktor-tasks-swagger/
├── src/main/resources/openapi/documentation.yaml
├── src/main/kotlin/ru/ithub/tasks/
│   ├── Application.kt
│   ├── plugins/StatusPages.kt      единая обработка ошибок
│   ├── plugins/RequestLogging.kt   CallLogging
│   ├── routes/TaskRoutes.kt        маршруты и Swagger UI
│   ├── security/                   BCrypt, JWT, Authentication
│   └── storage/
└── src/test/kotlin/                API, документация и журнал
```
