package com.github.snuk87.keycloak.kafka;

import org.keycloak.Config.Scope;

import java.util.Map;

public class KafkaProducerConfig extends KafkaConfig {

    public enum ProducerProperty {
        ACKS("acks"),
        BUFFER_MEMORY("buffer.memory"),
        COMPRESSION_TYPE("compression.type"),
        BATCH_SIZE("batch.size"),
        LINGER_MS("linger.ms"),
        MAX_REQUEST_SIZE("max.request.size"),
        PARTITIONER_CLASS("partitioner.class"),
        ENABLE_IDEMPOTENCE("enable.idempotence"),
        MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION("max.in.flight.requests.per.connection"),
        TRANSACTIONAL_ID("transactional.id"),
        DELIVERY_TIMEOUT_MS("delivery.timeout.ms"),
        MAX_BLOCK_MS("max.block.ms"),
        RETRY_BACKOFF_MS("retry.backoff.ms"),
        ;
        private final String name;
        ProducerProperty(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public static Map<String, Object> initProducer(Scope scope) {
        KafkaProperty[] common = KafkaProperty.values();
        ProducerProperty[] specific = ProducerProperty.values();

        Map<String, Object> props = KafkaConfig.init(scope, common);

        for (ProducerProperty p : specific) {
            String envValue = System.getenv("KAFKA_" + p.name());
            String scopeValue = scope.get(p.getName(), envValue);
            if (scopeValue != null) {
                props.put(p.getName(), scopeValue);
            }
        }

        return props;
    }
}
