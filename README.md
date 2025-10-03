# Keycloak Kafka Integration (EN)

The project is a modification of the original [Keycloak Kafka Event Listener](https://github.com/SnuK87/keycloak-kafka ) and adds support for custom events for integration with Apache Kafka.

## Changes and improvements

- Added support for the following Keycloak events:
- User registration (`REGISTER`)
- Email confirmation (`VERIFY_EMAIL`)
- Added custom Kafka topics:
- `CREATE_USER_TOPIC` — User creation events
- `VERIFY_EMAIL_TOPIC` — email confirmation events
- Implemented the Kafka producer via the `KafkaProducerFactory` with configuration support from environment variables.
- Added a new dependency for sending `userInfo` objects to Kafka.
- Added unit tests to verify the operation of the event listener with the mock producer.

## Configuration

New environment variables for connecting to Kafka:

- `KAFKA_CREATE_USER_TOPIC` — topic for creating a user
- `KAFKA_VERIFY_EMAIL_TOPIC` — topic for email confirmation

## Build and launch

```bash
mvn clean package
```
# Keycloak Kafka Integration (RU)

Проект является модификацией оригинального [Keycloak Kafka Event Listener](https://github.com/SnuK87/keycloak-kafka) и добавляет поддержку пользовательских событий для интеграции с Apache Kafka.

## Изменения и улучшения

- Добавлена поддержка следующих событий Keycloak:
    - Регистрация пользователя (`REGISTER`)
    - Подтверждение email (`VERIFY_EMAIL`)
- Добавлены пользовательские топики Kafka:
    - `CREATE_USER_TOPIC`  — события создания пользователя
    - `VERIFY_EMAIL_TOPIC` — события подтверждения email
- Реализован продюсер Kafka через `KafkaProducerFactory` с поддержкой конфигурации из переменных окружения.
- Добавлен новая зависмость для отправки объектов `UserInfo` в Kafka.
- Добавлены юнит-тесты для проверки работы слушателя событий с мок-продюсером.

## Конфигурация

Новые переменные окружения для подключения к Kafka:

- `KAFKA_CREATE_USER_TOPIC` — топик для создания пользователя
- `KAFKA_VERIFY_EMAIL_TOPIC` — топик для подтверждения email

## Сборка и запуск

```bash
mvn clean package
