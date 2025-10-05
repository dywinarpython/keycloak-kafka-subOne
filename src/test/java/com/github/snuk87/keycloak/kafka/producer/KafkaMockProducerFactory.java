package com.github.snuk87.keycloak.kafka.producer;

import java.util.Map;

import com.github.snuk87.keycloak.kafka.KafkaProducerFactory;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;

class KafkaMockProducerFactory implements KafkaProducerFactory {

	@Override
	public Producer<String, String> createProducer(String clientId, String bootstrapServer,
			Map<String, Object> optionalProperties) {
		return new MockProducer<>(true, new StringSerializer(), new StringSerializer());
	}

}
