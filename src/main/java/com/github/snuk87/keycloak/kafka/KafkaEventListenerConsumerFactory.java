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

	private volatile UserDeletionConsumer consumer;
	private Thread consumerThread;
	private final Object lock = new Object();

	@Override
	public EventListenerProvider create(KeycloakSession session) {
		return new UserDeletionEventListenerProvider();
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
		if (consumer == null) {
			synchronized (lock) {
				if (consumer == null) {
					LOG.info("Starting SINGLETON UserDeletionConsumer thread...");
					try {
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

						consumerThread.setDaemon(false);

						consumerThread.setUncaughtExceptionHandler((t, e) ->
							LOG.error("Uncaught exception in UserDeletionConsumer thread", e)
						);

						consumerThread.start();

						LOG.info("✓ SINGLETON UserDeletionConsumer thread started successfully");
					} catch (Exception e) {
						LOG.error("Failed to start UserDeletionConsumer", e);
						consumer = null;
						consumerThread = null;
						throw new RuntimeException("Cannot initialize Kafka consumer", e);
					}
				} else {
					LOG.warn("Consumer already initialized, skipping thread start");
				}
			}
		}
	}

	@Override
	public void close() {
		LOG.info("Shutting down KafkaEventListenerConsumerFactory...");

		if (consumer != null) {
			synchronized (lock) {
				if (consumer != null) {
					LOG.info("Stopping UserDeletionConsumer...");

					try {
						consumer.stop();

						if (consumerThread != null && consumerThread.isAlive()) {
							LOG.info("Waiting for consumer thread to finish...");
							consumerThread.join(10000);
							if (consumerThread.isAlive()) {
								LOG.warn("Consumer thread did not stop in time, interrupting...");
								consumerThread.interrupt();

								consumerThread.join(2000);
								if (consumerThread.isAlive()) {
									LOG.error("Consumer thread still alive after interrupt!");
								} else {
									LOG.info("Consumer thread stopped after interrupt");
								}
							} else {
								LOG.info("✓ Consumer thread stopped gracefully");
							}
						}
					} catch (InterruptedException e) {
						LOG.warn("Interrupted while waiting for consumer to stop", e);
						Thread.currentThread().interrupt();
					} finally {
						consumer = null;
						consumerThread = null;
					}
				}
			}
		}

		LOG.info("KafkaEventListenerConsumerFactory shutdown complete");
	}
}