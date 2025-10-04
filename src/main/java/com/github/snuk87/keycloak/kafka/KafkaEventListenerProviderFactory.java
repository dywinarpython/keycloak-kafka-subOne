package com.github.snuk87.keycloak.kafka;

import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class KafkaEventListenerProviderFactory implements EventListenerProviderFactory {

	private static final Logger LOG = Logger.getLogger(KafkaEventListenerProviderFactory.class);
	private static final String ID = "kafka";

	private KafkaEventListenerProvider instance;

	private String bootstrapServers;
	private String topicCreateUser;
	private String topicVerifyEmail;
	private String topicEvents;
	private String topicAdminEvents;
	private String clientId;
	private String[] events;
	private Map<String, Object> kafkaProducerProperties;

	@Override
	public EventListenerProvider create(KeycloakSession session) {
		if (instance == null) {
			instance = new KafkaEventListenerProvider(bootstrapServers, clientId, topicEvents, events, topicAdminEvents,
					kafkaProducerProperties, new KafkaStandardProducerImpl(), session, topicCreateUser, topicVerifyEmail);
		}
		return instance;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void init(Scope config) {
		LOG.info("Init kafka module ...");
		topicEvents = config.get("topicEvents", System.getenv("KAFKA_TOPIC"));

		topicCreateUser = System.getenv("KAFKA_CREATE_USER_TOPIC");

		topicVerifyEmail = System.getenv("KAFKA_VERIFY_EMAIL_TOPIC");

		clientId = config.get("clientId", System.getenv("KAFKA_CLIENT_ID"));
		bootstrapServers = config.get("bootstrapServers", System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
		topicAdminEvents = config.get("topicAdminEvents", System.getenv("KAFKA_ADMIN_TOPIC"));


		String eventsString = config.get("events", System.getenv("KAFKA_EVENTS"));

		if(topicCreateUser == null){
			throw new NullPointerException("topic create_user must be not null.");
		}
		LOG.info("CREATE_USER_TOPIC: " + topicCreateUser);

		if(topicVerifyEmail == null){
			throw new NullPointerException("topic verify_email must be not null.");
		}
		LOG.info("VERIFY_EMAIL_TOPIC: " + topicVerifyEmail);

		if (eventsString != null) {
			events = eventsString.split(",");
		}
		LOG.info("EVENTS: " +eventsString);
		if (topicEvents == null) {
			LOG.warn("Additional topics will not be transmitted, the default topics will be used: VERIFY_EMAIL_TOPIC, CREATE_USER_TOPIC");
		} else {
			LOG.info("TOPIC_EVENTS: " + topicEvents);
		}
		if (clientId == null) {
			throw new NullPointerException("clientId must not be null.");
		}
		LOG.info("CLIENT_ID: " + clientId);

		if (bootstrapServers == null) {
			throw new NullPointerException("bootstrapServers must not be null");
		}

		LOG.info("BOOTSTRAP_SERVERS: " + bootstrapServers);
		kafkaProducerProperties = KafkaProducerConfig.initProducer(config);
	}

	@Override
	public void postInit(KeycloakSessionFactory arg0) {
		// ignore
	}

	@Override
	public void close() {
		// ignore
	}
}
