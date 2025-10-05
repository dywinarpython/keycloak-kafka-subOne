package com.github.snuk87.keycloak.kafka.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;

import com.github.snuk87.keycloak.kafka.KafkaEventListenerProvider;
import com.github.snuk87.keycloak.kafka.KafkaProducerFactory;
import org.apache.kafka.clients.producer.MockProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import static org.mockito.Mockito.*;

class KafkaEventListenerProviderTests {

	private KafkaEventListenerProvider listener;

    @BeforeEach
	void setUp() {
		KeycloakSession mockSession = mock(KeycloakSession.class);
		UserProvider mockUsers = mock(UserProvider.class);
		UserModel mockUser = mock(UserModel.class);

		when(mockSession.users()).thenReturn(mockUsers);
		when(mockUsers.getUserById(any(), anyString())).thenReturn(mockUser);
		when(mockUser.getFirstName()).thenReturn("John");
		when(mockUser.getLastName()).thenReturn("Doe");
		when(mockUser.getEmail()).thenReturn("john.doe@example.com");
		when(mockUser.isEmailVerified()).thenReturn(true);
		when(mockUser.getId()).thenReturn("user-id-123");

        KafkaProducerFactory factory = new KafkaMockProducerFactory();
		listener = new KafkaEventListenerProvider("", "", "", new String[] { "REGISTER" }, "admin-events", Map.of(),
                factory, mockSession, "create_user", "verify_email");
	}

	@Test
	void shouldProduceEventWhenTypeIsDefined() throws Exception {
		Event mockEvent = mock(Event.class);
		when(mockEvent.getType()).thenReturn(EventType.REGISTER);
		when(mockEvent.getUserId()).thenReturn("935edd54-9d81-48fb-b114-8c5144367630");
		when(mockEvent.getDetails()).thenReturn(Map.of(
				"first_name", "John",
				"last_name", "Doe",
				"email", "john.doe@example.com"
		));

		MockProducer<?, ?> producer = getProducerUsingReflection();

		listener.onEvent(mockEvent);

		assertEquals(1, producer.history().size());
	}

	@Test
	void shouldDoNothingWhenTypeIsNotDefined() throws Exception {
		Event event = new Event();
		event.setType(EventType.CLIENT_DELETE);
		MockProducer<?, ?> producer = getProducerUsingReflection();

		listener.onEvent(event);

		assertTrue(producer.history().isEmpty());
	}

	@Test
	void shouldProduceEventWhenTopicAdminEventsIsNotNull() throws Exception {
		AdminEvent event = new AdminEvent();
		MockProducer<?, ?> producer = getProducerUsingReflection();

		listener.onEvent(event, false);

		assertEquals(1, producer.history().size());
	}

	@Test
	void shouldDoNothingWhenTopicAdminEventsIsNull() throws Exception {
		Field field = listener.getClass().getDeclaredField("topicAdminEvents");
		field.setAccessible(true);
		field.set(listener, null);
		AdminEvent event = new AdminEvent();
		MockProducer<?, ?> producer = getProducerUsingReflection();
		listener.onEvent(event, false);

		assertTrue(producer.history().isEmpty());
	}

	private MockProducer<?, ?> getProducerUsingReflection() throws Exception {
		Field producerField = KafkaEventListenerProvider.class.getDeclaredField("producer");
		producerField.setAccessible(true);
		return (MockProducer<?, ?>) producerField.get(listener);
	}

}
