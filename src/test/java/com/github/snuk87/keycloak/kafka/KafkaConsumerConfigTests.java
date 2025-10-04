package com.github.snuk87.keycloak.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.keycloak.Config.SystemPropertiesConfigProvider;

class KafkaConsumerConfigTests {

	@Test
	void shouldReturnMapWithConsumerConfigWhenPropertyExists() {
		System.setProperty("keycloak.session.timeout.ms", "30000");
		System.setProperty("keycloak.heartbeat.interval.ms", "10000");
		System.setProperty("keycloak.foo", "bar");

		Map<String, Object> config = KafkaConsumerConfig.initConsumer(new SystemPropertiesConfigProvider().scope());

		assertEquals("30000", config.get("session.timeout.ms"));
		assertEquals("10000", config.get("heartbeat.interval.ms"));
		assertNull(config.get("keycloak.foo"));
	}
}
