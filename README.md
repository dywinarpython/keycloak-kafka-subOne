# Keycloak Kafka Integration (EN)

The project is a modification of the original [Keycloak Kafka Event Listener](https://github.com/SnuK87/keycloak-kafka) and adds support for custom events for integration with Apache Kafka.

## Changes and improvements

### Kafka Producer (Event Publishing)
- Added support for the following Keycloak events:
  - User registration (`REGISTER`)
  - Email confirmation (`VERIFY_EMAIL`)
- Added custom Kafka topics:
  - `CREATE_USER_TOPIC` — User creation events
  - `VERIFY_EMAIL_TOPIC` — Email confirmation events
- Implemented the Kafka producer via the `KafkaProducerFactory` with configuration support from environment variables.
- Added a new dependency for sending `userInfo` objects to Kafka.
- Added unit tests to verify the operation of the event listener with the mock producer.

### Kafka Consumer (User Deletion) 🆕
- **Added Kafka Consumer for automatic user deletion**
- Consumer listens to a specified Kafka topic and automatically deletes users from Keycloak when receiving `userId`
- Features:
  - **Transaction support** — guaranteed data consistency
  - **Graceful shutdown** — correct consumer termination when Keycloak stops
- Consumer runs in a separate background thread and works independently of HTTP sessions

## Configuration

### Producer Configuration
Environment variables for Kafka producer:

- `KAFKA_CREATE_USER_TOPIC` — topic for user creation events
- `KAFKA_VERIFY_EMAIL_TOPIC` — topic for email confirmation events
- `KAFKA_BOOTSTRAP_SERVERS` — Kafka broker addresses (e.g., `kafka1:9092,kafka2:9093,kafka3:9094`)

### Consumer Configuration 🆕
**Required** environment variables for Kafka consumer:

```yaml
KAFKA_BOOTSTRAP_SERVERS: some-bootstrap-servers
KAFKA_DELETE_USER_TOPIC: name-topik
KAFKA_REALM_NAME: realName
KAFKA_GROUP_ID: keycloak-user-deletion-group
```


## Usage

### Sending User Deletion Request

To delete a user, send the user's UUID to the configured Kafka topic:

```bash
# Example: Send userId to Kafka topic
echo "c32ec43e-659d-4a91-812e-3f601b23e24b" | kafka-console-producer \
  --broker-list kafka1:9092 \
  --topic delete_user
```

The consumer will:
1. Receive the message from the topic
2. Find the user in the specified realm
3. Delete the user from Keycloak
4. Commit the offset only after successful deletion

### Message Format

The consumer expects a simple string message containing the user's UUID:

```
c32ec43e-659d-4a91-812e-3f601b23e24b
```

### Monitoring

Check consumer status in Keycloak logs:

```bash
# Consumer started successfully
INFO [UserDeletionConsumer] UserDeletionConsumer thread started successfully
INFO [UserDeletionConsumer] Subscribed to topic: delete_user

# Message processing
INFO [UserDeletionConsumer] Received 1 message(s) from topic 'delete_user'
INFO [UserDeletionConsumer] Processing user deletion: userId='xxx', partition=1, offset=4
INFO [UserDeletionConsumer] ✓ User successfully deleted: userId='xxx', username='user@example.com'
```

## Build and deployment

```bash
mvn clean package
```

The resulting JAR file should be placed in the Keycloak providers directory:

```bash
cp target/keycloak-kafka-*.jar /opt/keycloak/providers/
```

## Architecture

- **Producer**: Sends user creation and email verification events to Kafka
- **Consumer**: Listens to user deletion topic and removes users from Keycloak
- **Thread-safe**: Safe concurrent operation with multiple Keycloak nodes
- **Transactional**: Guarantees data consistency with Keycloak database

## Security Considerations

- ⚠️ **Important**: The consumer deletes users without additional authorization checks
- Ensure your Kafka topic is properly secured with ACLs
- Consider implementing additional validation logic in `deleteUser()` method
- Use Kafka SSL/SASL for production environments

---

# Keycloak Kafka Integration (RU)

Проект является модификацией оригинального [Keycloak Kafka Event Listener](https://github.com/SnuK87/keycloak-kafka) и добавляет поддержку пользовательских событий для интеграции с Apache Kafka.

## Изменения и улучшения

### Kafka Producer (Публикация событий)
- Добавлена поддержка следующих событий Keycloak:
  - Регистрация пользователя (`REGISTER`)
  - Подтверждение email (`VERIFY_EMAIL`)
- Добавлены пользовательские топики Kafka:
  - `CREATE_USER_TOPIC` — события создания пользователя
  - `VERIFY_EMAIL_TOPIC` — события подтверждения email
- Реализован продюсер Kafka через `KafkaProducerFactory` с поддержкой конфигурации из переменных окружения
- Добавлена новая зависимость для отправки объектов `UserInfo` в Kafka
- Добавлены юнит-тесты для проверки работы слушателя событий с мок-продюсером

### Kafka Consumer (Удаление пользователей) 🆕
- **Добавлен Kafka Consumer для автоматического удаления пользователей**
- Consumer слушает указанный топик Kafka и автоматически удаляет пользователей из Keycloak при получении `userId`
- Возможности:
  - **Поддержка транзакций** — гарантированная консистентность данных
  - **Graceful shutdown** — корректное завершение consumer при остановке Keycloak
- Consumer работает в отдельном фоновом потоке и функционирует независимо от HTTP-сессий

## Конфигурация

### Конфигурация Producer
Переменные окружения для Kafka producer:

- `KAFKA_CREATE_USER_TOPIC` — топик для событий создания пользователя
- `KAFKA_VERIFY_EMAIL_TOPIC` — топик для событий подтверждения email
- `KAFKA_BOOTSTRAP_SERVERS` — адреса Kafka брокеров (например, `kafka1:9092,kafka2:9093,kafka3:9094`)

### Конфигурация Consumer 🆕
**Обязательные** переменные окружения для Kafka consumer:

```yaml
KAFKA_BOOTSTRAP_SERVERS: some-bootstrap-servers
KAFKA_DELETE_USER_TOPIC: name-topik
KAFKA_REALM_NAME: realName
KAFKA_GROUP_ID: keycloak-user-deletion-group
```

## Использование

### Отправка запроса на удаление пользователя

Для удаления пользователя отправьте UUID пользователя в настроенный топик Kafka:

```bash
# Пример: Отправка userId в топик Kafka
echo "c32ec43e-659d-4a91-812e-3f601b23e24b" | kafka-console-producer \
  --broker-list kafka1:9092 \
  --topic delete_user
```

Consumer выполнит следующие действия:
1. Получит сообщение из топика
2. Найдет пользователя в указанном realm
3. Удалит пользователя из Keycloak
4. Закоммитит offset только после успешного удаления

### Формат сообщения

Consumer ожидает простое строковое сообщение, содержащее UUID пользователя:

```
c32ec43e-659d-4a91-812e-3f601b23e24b
```

### Мониторинг

Проверить статус consumer можно в логах Keycloak:

```bash
# Consumer успешно запущен
INFO [UserDeletionConsumer] UserDeletionConsumer thread started successfully
INFO [UserDeletionConsumer] Subscribed to topic: delete_user

# Обработка сообщения
INFO [UserDeletionConsumer] Received 1 message(s) from topic 'delete_user'
INFO [UserDeletionConsumer] Processing user deletion: userId='xxx', partition=1, offset=4
INFO [UserDeletionConsumer] ✓ User successfully deleted: userId='xxx', username='user@example.com'
```

## Сборка и развертывание

```bash
mvn clean package
```

Полученный JAR-файл необходимо поместить в директорию провайдеров Keycloak:

```bash
cp target/keycloak-kafka-*.jar /opt/keycloak/providers/
```

## Архитектура

- **Producer**: Отправляет события создания пользователей и подтверждения email в Kafka
- **Consumer**: Слушает топик удаления пользователей и удаляет их из Keycloak
- **Thread-safe**: Безопасная работа при множественных узлах Keycloak
- **Transactional**: Гарантирует консистентность данных с базой Keycloak

## Соображения безопасности

- ⚠️ **Важно**: Consumer удаляет пользователей без дополнительных проверок авторизации
- Убедитесь, что ваш топик Kafka защищен с помощью ACL
- Рассмотрите возможность добавления дополнительной логики валидации в метод `deleteUser()`
- Используйте Kafka SSL/SASL для production окружений
- Ограничьте доступ к топику удаления только доверенным сервисам
