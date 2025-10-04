package com.github.snuk87.keycloak.kafka;

import org.apache.kafka.clients.consumer.Consumer;

import java.util.Map;

public interface KafkaConsumerFactory {

	<K, V> Consumer<K, V> createConsumer(String clientId,
										 String bootstrapServer,
										 Map<String, Object> optionalProperties);

}
