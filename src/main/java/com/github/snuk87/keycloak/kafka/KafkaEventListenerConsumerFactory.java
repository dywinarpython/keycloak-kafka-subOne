package com.github.snuk87.keycloak.kafka;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.Map;

public class KafkaEventListenerConsumerFactory implements EventListenerProviderFactory {
	private static final Logger LOG = Logger.getLogger(KafkaEventListenerConsumerFactory.class);
	private static final String ID = "kafka-consumer-delete-user";

	private KafkaConsumerFactory consumerFactory;
	private String bootstrapServers;
	private String realmName;
	private String topicDeleteUser;
	private Map<String, Object> kafkaConsumerProperties;

	private UserDeletionConsumer consumer;
	private Thread consumerThread;

	@Override
	public EventListenerProvider create(KeycloakSession session) {
		LOG.info("Creating new UserDeletionConsumer for session");
		UserDeletionConsumer newConsumer = new UserDeletionConsumer(
				consumerFactory,
				"keycloak-user-deletion-consumer-" + System.currentTimeMillis(),
				bootstrapServers,
				kafkaConsumerProperties,
				session.getKeycloakSessionFactory(),
				realmName,
				topicDeleteUser
		);
		return new UserDeletionEventListenerProvider(newConsumer);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void init(Scope config) {
		LOG.info("Init kafka consumer module for user deletion...");

		bootstrapServers = config.get("bootstrapServers", System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
		realmName = config.get("realmName", System.getenv("KAFKA_REALM_NAME"));
		topicDeleteUser = config.get("topicDeleteUser", System.getenv("KAFKA_DELETE_USER_TOPIC"));

		if (bootstrapServers == null) {
			throw new NullPointerException("bootstrapServers must not be null");
		}

		if (realmName == null) {
			realmName = "master";
			LOG.warn("REALM_NAME not set, using default: master");
		}

		if (topicDeleteUser == null) {
			topicDeleteUser = "delete_user";
			LOG.warn("DELETE_USER_TOPIC not set, using default: delete_user");
		}

		LOG.info("BOOTSTRAP_SERVERS: " + bootstrapServers);
		LOG.info("REALM_NAME: " + realmName);
		LOG.info("DELETE_USER_TOPIC: " + topicDeleteUser);

		kafkaConsumerProperties = KafkaConsumerConfig.initConsumer(config);
		consumerFactory = new KafkaConsumerFactoryImpl();

		LOG.info("Kafka consumer module initialized successfully");
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		LOG.info("Starting UserDeletionConsumer thread...");

		if (consumer == null) {
			consumer = new UserDeletionConsumer(
					consumerFactory,
					"keycloak-user-deletion-consumer-" + System.currentTimeMillis(),
					bootstrapServers,
					kafkaConsumerProperties,
					factory,
					realmName,
					topicDeleteUser
			);
			consumerThread = new Thread(consumer, "UserDeletionConsumer-Thread");
			consumerThread.setDaemon(true);
			consumerThread.start();
			LOG.info("UserDeletionConsumer thread started successfully");
		} else {
			LOG.warn("Consumer already initialized, skipping thread start");
		}
	}

	@Override
	public void close() {
		LOG.info("Shutting down UserDeletionConsumer...");

		if (consumer != null) {
			consumer.stop();
			if (consumerThread != null) {
				try {
					consumerThread.join(5000);
					if (consumerThread.isAlive()) {
						LOG.warn("Consumer thread did not stop in time, interrupting...");
						consumerThread.interrupt();
					} else {
						LOG.info("Consumer thread stopped successfully");
					}
				} catch (InterruptedException e) {
					LOG.warn("Interrupted while waiting for consumer to stop", e);
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}