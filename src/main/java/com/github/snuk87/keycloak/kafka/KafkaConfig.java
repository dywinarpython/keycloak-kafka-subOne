package com.github.snuk87.keycloak.kafka;

import java.util.HashMap;
import java.util.Map;
import org.keycloak.Config.Scope;

public abstract class KafkaConfig {

  public static Map<String, Object> init(Scope scope, KafkaProperty[] properties) {
    Map<String, Object> propertyMap = new HashMap<>();

    for (KafkaProperty property : properties) {
      String envValue = System.getenv("KAFKA_" + property.name());
      String scopeValue = scope.get(property.getName(), envValue);

      if (property == KafkaProperty.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM &&
              "disabled".equalsIgnoreCase(scopeValue)) {
        propertyMap.put(property.getName(), "");
      } else if (scopeValue != null) {
        propertyMap.put(property.getName(), scopeValue);
      }
    }

    return propertyMap;
  }

  public enum KafkaProperty {
    BOOTSTRAP_SERVERS("bootstrap.servers"),
    SECURITY_PROTOCOL("security.protocol"),
    SASL_MECHANISM("sasl.mechanism"),
    SSL_TRUSTSTORE_LOCATION("ssl.truststore.location"),
    SSL_TRUSTSTORE_PASSWORD("ssl.truststore.password"),
    SSL_KEYSTORE_LOCATION("ssl.keystore.location"),
    SSL_KEYSTORE_PASSWORD("ssl.keystore.password"),
    SSL_KEY_PASSWORD("ssl.key.password"),
    SSL_ENDPOINT_IDENTIFICATION_ALGORITHM("ssl.endpoint.identification.algorithm"),
    SESSION_TIMEOUT_MS("session.timeout.ms"),
    HEARTBEAT_INTERVAL_MS("heartbeat.interval.ms"),
    ;
    private final String name;
    KafkaProperty(String name) { this.name = name; }
    public String getName() { return name; }
  }
}
