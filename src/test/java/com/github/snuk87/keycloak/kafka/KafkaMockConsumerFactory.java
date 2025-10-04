package com.github.snuk87.keycloak.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.util.Map;

class KafkaMockConsumerFactory implements KafkaConsumerFactory {

	@Override
	public <K, V> Consumer<K, V> createConsumer(String clientId,
												String bootstrapServer,
												Map<String, Object> optionalProperties) {
		return new MockConsumer<K, V>(OffsetResetStrategy.EARLIEST);
	}
}
