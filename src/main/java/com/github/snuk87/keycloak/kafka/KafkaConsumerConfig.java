package com.github.snuk87.keycloak.kafka;

import org.keycloak.Config.Scope;

import java.util.Map;

public class KafkaConsumerConfig extends KafkaConfig {

    public enum ConsumerProperty {
        GROUP_ID("group.id"),
        ENABLE_AUTO_COMMIT("enable.auto.commit"),
        AUTO_OFFSET_RESET("auto.offset.reset"),
        FETCH_MIN_BYTES("fetch.min.bytes"),
        FETCH_MAX_BYTES("fetch.max.bytes"),
        MAX_POLL_RECORDS("max.poll.records"),
        ;
        private final String name;
        ConsumerProperty(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public static Map<String, Object> initConsumer(Scope scope) {
        KafkaProperty[] common = KafkaProperty.values();
        ConsumerProperty[] specific = ConsumerProperty.values();

        Map<String, Object> props = KafkaConfig.init(scope, common);

        for (ConsumerProperty c : specific) {
            String envValue = System.getenv("KAFKA_" + c.name());
            String scopeValue = scope.get(c.getName(), envValue);
            if (scopeValue != null) {
                props.put(c.getName(), scopeValue);
            }
        }

        return props;
    }
}
