package com.github.snuk87.keycloak.kafka.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import com.github.snuk87.keycloak.kafka.KafkaProducerConfig;
import org.junit.jupiter.api.Test;

import org.keycloak.Config.SystemPropertiesConfigProvider;

class KafkaProducerConfigTests {

	@Test
	void shouldReturnMapWithConfigWhenPropertyExists() {
		System.setProperty("keycloak.retry.backoff.ms", "1000");
		System.setProperty("keycloak.max.block.ms", "5000");
		System.setProperty("keycloak.foo", "bar");

		Map<String, Object> config = KafkaProducerConfig.initProducer(new SystemPropertiesConfigProvider().scope());

		assertEquals("5000", config.get("max.block.ms"));
		assertEquals("1000", config.get("retry.backoff.ms"));
        assertNull(config.get("keycloak.foo"));
	}
}
