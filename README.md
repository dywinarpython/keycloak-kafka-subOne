# Keycloak Kafka Integration

Проект является модификацией оригинального [Keycloak Kafka Event Listener](https://github.com/SnuK87/keycloak-kafka) и добавляет поддержку пользовательских событий для интеграции с Apache Kafka.

## Изменения и улучшения

- Добавлена поддержка следующих событий Keycloak:
  - Регистрация пользователя (`REGISTER`)
  - Первый вход через провайдера идентификации (`IDENTITY_PROVIDER_FIRST_LOGIN`)
  - Подтверждение email (`VERIFY_EMAIL`)
- Добавлены пользовательские топики Kafka:
  - `CREATE_USER_TOPIC` — события создания пользователя
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
