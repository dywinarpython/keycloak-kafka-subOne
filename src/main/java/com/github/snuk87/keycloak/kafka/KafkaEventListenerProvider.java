package com.github.snuk87.keycloak.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.subOne.kecyloak_dto.UserInfo;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class KafkaEventListenerProvider implements EventListenerProvider {

	private static final Logger LOG = Logger.getLogger(KafkaEventListenerProvider.class);

	private final String topicEvents;

	private final String topicCreateUser;
	private final String topicVerifyEmail;

	private final KeycloakSession keycloakSession;

	private final List<EventType> events;

	private final String topicAdminEvents;

	private final Producer<String, String> producer;

	private final ObjectMapper mapper;

	public KafkaEventListenerProvider(String bootstrapServers, String clientId, String topicEvents, String[] events,
			String topicAdminEvents, Map<String, Object> kafkaProducerProperties, KafkaProducerFactory factory, KeycloakSession session, String topicCreateUser,
	String topicVerifyEmail) {
		this.topicEvents = topicEvents;
		this.events = new ArrayList<>();
		this.topicAdminEvents = topicAdminEvents;
		this.keycloakSession = session;
		this.topicCreateUser = topicCreateUser;
		this.topicVerifyEmail = topicVerifyEmail;
		if(events != null) {
			for (String event : events) {
				try {
					EventType eventType = EventType.valueOf(event.toUpperCase());
					this.events.add(eventType);
				} catch (IllegalArgumentException e) {
					LOG.debug("Ignoring event >" + event + "<. Event does not exist.");
				}
			}
		}
		producer = factory.createProducer(clientId, bootstrapServers, kafkaProducerProperties);
		mapper = new ObjectMapper();
	}

	private void produceEvent(String eventAsString, String key, String topic)
			throws InterruptedException, ExecutionException, TimeoutException {
		LOG.debug("Produce to topic: " + topic + " ...");
		ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, eventAsString);
		Future<RecordMetadata> metaData = producer.send(record);
		RecordMetadata recordMetadata = metaData.get(30, TimeUnit.SECONDS);
		LOG.debug("Produced to topic: " + recordMetadata.topic());
	}

	@Override
	public void onEvent(Event event) {
		try {
			if (event.getType().equals(EventType.REGISTER)) {
				Map<String, String> details = event.getDetails();
				String firstName;
				String lastName;
				if(details.get("identity_provider") == null) {
					firstName = details.get("first_name");
					lastName = details.get("last_name");
				} else {
					RealmModel realm = keycloakSession.realms().getRealm(event.getRealmId());
					UserModel user = keycloakSession.users().getUserById(realm, event.getUserId());
					firstName = user.getFirstName();
					lastName = user.getLastName();
				}
				String email = details.get("email");
				String userId = event.getUserId();
				UserInfo userInfo = new UserInfo(
							firstName,
							lastName,
							UUID.fromString(userId),
							email,
							false
				);
				produceEvent(mapper.writeValueAsString(userInfo), event.getUserId(), topicCreateUser);
			} else if (event.getType().equals(EventType.VERIFY_EMAIL)) {
				Map<String, String> details = event.getDetails();
				String email = details.get("email");
				produceEvent(email, event.getUserId(), topicVerifyEmail);
			} else if(events.contains(event.getType())){
				produceEvent(mapper.writeValueAsString(event), event.getUserId(), topicEvents);
			}
		} catch (JsonProcessingException | ExecutionException | TimeoutException e) {
			LOG.error(e.getMessage(), e);
		} catch (InterruptedException e) {
			LOG.error(e.getMessage(), e);
			Thread.currentThread().interrupt();
		}
	}
	@Override
	public void onEvent(AdminEvent event, boolean includeRepresentation) {
		if (topicAdminEvents != null) {
			try {
				produceEvent(mapper.writeValueAsString(event), null, topicAdminEvents);
			} catch (JsonProcessingException | ExecutionException | TimeoutException e) {
				LOG.error(e.getMessage(), e);
			} catch (InterruptedException e) {
				LOG.error(e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public void close() {
		// ignore
	}
}
