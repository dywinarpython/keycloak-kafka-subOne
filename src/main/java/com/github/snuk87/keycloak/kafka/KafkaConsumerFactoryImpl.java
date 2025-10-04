package com.github.snuk87.keycloak.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Map;
import java.util.Properties;

public class KafkaConsumerFactoryImpl implements KafkaConsumerFactory {

    @Override
    public <K, V> Consumer<K, V> createConsumer(String clientId,
                                                String bootstrapServer,
                                                Map<String, Object> optionalProperties) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        if (optionalProperties != null) {
            props.putAll(optionalProperties);
        }
        return new KafkaConsumer<>(props);
    }
}
