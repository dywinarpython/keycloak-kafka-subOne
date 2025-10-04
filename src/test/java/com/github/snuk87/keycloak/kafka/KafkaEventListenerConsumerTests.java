package com.github.snuk87.keycloak.kafka;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.*;

class KafkaEventListenerConsumerTests {


	private MockConsumer<String, String> mockConsumer;
	private KeycloakSession mockSession;
	private RealmProvider mockRealmProvider;
	private UserProvider mockUserProvider;
	private RealmModel mockRealm;
	private UserModel mockUser;

	private UserDeletionConsumer consumer;
	private Thread consumerThread;

	private static final String REALM_NAME = "test-realm";
	private static final String TOPIC_NAME = "delete_user";
	private static final String USER_ID = "user-123";

	@BeforeEach
	void setUp() {
		// Mock Kafka Consumer
		mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
		mockConsumer.subscribe(Collections.singletonList(TOPIC_NAME));
		TopicPartition partition = new TopicPartition(TOPIC_NAME, 0);
		mockConsumer.rebalance(Collections.singletonList(partition));
		mockConsumer.updateBeginningOffsets(Map.of(partition, 0L));


		mockSession = mock(KeycloakSession.class);
		mockRealmProvider = mock(RealmProvider.class);
		mockUserProvider = mock(UserProvider.class);
		mockRealm = mock(RealmModel.class);
		mockUser = mock(UserModel.class);
		KeycloakTransactionManager mockTx = mock(KeycloakTransactionManager.class);

		when(mockSession.getTransactionManager()).thenReturn(mockTx);
		when(mockSession.realms()).thenReturn(mockRealmProvider);
		when(mockSession.users()).thenReturn(mockUserProvider);
		when(mockRealmProvider.getRealmByName(REALM_NAME)).thenReturn(mockRealm);

		KeycloakSessionFactory mockSessionFactory = mock(KeycloakSessionFactory.class);
		when(mockSessionFactory.create()).thenReturn(mockSession);
		when(mockSession.getKeycloakSessionFactory()).thenReturn(mockSessionFactory);

		KafkaConsumerFactory factory = new KafkaConsumerFactory() {
			@Override
			@SuppressWarnings("unchecked")
			public <K, V> Consumer<K, V> createConsumer(String clientId, String bootstrapServer, Map<String, Object> optionalProperties) {
				return (Consumer<K, V>) mockConsumer;
			}
		};

		consumer = new UserDeletionConsumer(
				 factory,
				"test-client",
				"localhost:9092",
				new HashMap<>(),
				mockSession.getKeycloakSessionFactory(),
				REALM_NAME,
				TOPIC_NAME
		);
	}

	@AfterEach
	void tearDown() {
		if (consumer != null) {
			consumer.stop();
		}
		if (consumerThread != null && consumerThread.isAlive()) {
			consumerThread.interrupt();
		}
	}

	@Test
	void shouldDeleteUserSuccessfully() {
		// Given
		when(mockUserProvider.getUserById(mockRealm, USER_ID)).thenReturn(mockUser);
		when(mockUserProvider.removeUser(mockRealm, mockUser)).thenReturn(true);
		when(mockUser.getUsername()).thenReturn("testuser");
		when(mockUser.getEmail()).thenReturn("test@example.com");
		mockConsumer.addRecord(new ConsumerRecord<>(TOPIC_NAME, 0, 0L, USER_ID, USER_ID));
		// When
		consumerThread = new Thread(() -> {
			try {
				Thread.sleep(10);
				consumer.stop();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		consumerThread.start();
		consumer.run();

		// Then
		verify(mockUserProvider).getUserById(mockRealm, USER_ID);
		verify(mockUserProvider).removeUser(mockRealm, mockUser);
	}

	@Test
	void shouldHandleUserNotFound() {
		// Given
		when(mockUserProvider.getUserById(mockRealm, USER_ID)).thenReturn(null);

		// Add message
		mockConsumer.addRecord(new ConsumerRecord<>(TOPIC_NAME, 0, 0L, USER_ID, USER_ID));

		// When
		consumerThread = new Thread(() -> {
			try {
				Thread.sleep(100);
				consumer.stop();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		consumerThread.start();
		consumer.run();

		// Then
		verify(mockUserProvider).getUserById(mockRealm, USER_ID);
		verify(mockUserProvider, never()).removeUser(any(), any());
	}

	@Test
	void shouldHandleRealmNotFound() {
		// Given
		when(mockRealmProvider.getRealmByName(REALM_NAME)).thenReturn(null);

		// Add message
		mockConsumer.addRecord(new ConsumerRecord<>(TOPIC_NAME, 0, 0L, USER_ID, USER_ID));

		// When
		consumerThread = new Thread(() -> {
			try {
				Thread.sleep(100);
				consumer.stop();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		consumerThread.start();
		consumer.run();


		verify(mockRealmProvider).getRealmByName(REALM_NAME);
		verify(mockUserProvider, never()).getUserById(any(), any());
	}

	@Test
	void shouldHandleNullUserId() {
		// Given - null userId
		mockConsumer.addRecord(new ConsumerRecord<>(TOPIC_NAME, 0, 0L, null, null));

		// When
		consumerThread = new Thread(() -> {
			try {
				Thread.sleep(100);
				consumer.stop();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		consumerThread.start();
		consumer.run();

		// Then
		verify(mockUserProvider, never()).getUserById(any(), any());
	}

	@Test
	void shouldHandleEmptyUserId() {
		// Given - empty userId
		mockConsumer.addRecord(new ConsumerRecord<>(TOPIC_NAME, 0, 0L, "", ""));

		// When
		consumerThread = new Thread(() -> {
			try {
				Thread.sleep(100);
				consumer.stop();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		consumerThread.start();
		consumer.run();

		// Then
		verify(mockUserProvider, never()).getUserById(any(), any());
	}

	@Test
	void shouldHandleDeleteFailure() {
		// Given
		when(mockUserProvider.getUserById(mockRealm, USER_ID)).thenReturn(mockUser);
		when(mockUserProvider.removeUser(mockRealm, mockUser)).thenReturn(false);
		when(mockUser.getUsername()).thenReturn("testuser");

		// Add message
		mockConsumer.addRecord(new ConsumerRecord<>(TOPIC_NAME, 0, 0L, USER_ID, USER_ID));

		// When
		consumerThread = new Thread(() -> {
			try {
				Thread.sleep(100);
				consumer.stop();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		consumerThread.start();
		consumer.run();

		// Then
		verify(mockUserProvider).removeUser(mockRealm, mockUser);
	}

	@Test
	void shouldProcessMultipleMessages() {
		// Given
		String userId1 = "user-1";
		String userId2 = "user-2";

		UserModel user1 = mock(UserModel.class);
		UserModel user2 = mock(UserModel.class);

		when(mockUserProvider.getUserById(mockRealm, userId1)).thenReturn(user1);
		when(mockUserProvider.getUserById(mockRealm, userId2)).thenReturn(user2);
		when(mockUserProvider.removeUser(mockRealm, user1)).thenReturn(true);
		when(mockUserProvider.removeUser(mockRealm, user2)).thenReturn(true);
		when(user1.getUsername()).thenReturn("user1");
		when(user2.getUsername()).thenReturn("user2");

		// Add multiple messages
		mockConsumer.addRecord(new ConsumerRecord<>(TOPIC_NAME, 0, 0L, userId1, userId1));
		mockConsumer.addRecord(new ConsumerRecord<>(TOPIC_NAME, 0, 1L, userId2, userId2));

		// When
		consumerThread = new Thread(() -> {
			try {
				Thread.sleep(200);
				consumer.stop();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		consumerThread.start();
		consumer.run();

		// Then
		verify(mockUserProvider).getUserById(mockRealm, userId1);
		verify(mockUserProvider).getUserById(mockRealm, userId2);
		verify(mockUserProvider).removeUser(mockRealm, user1);
		verify(mockUserProvider).removeUser(mockRealm, user2);
	}

	@Test
	void shouldStopGracefully() throws InterruptedException {
		// When
		consumerThread = new Thread(consumer);
		consumerThread.start();

		Thread.sleep(50); // Let it start
		consumer.stop();

		consumerThread.join(1000); // Wait for stop

		// Then
		assertFalse(consumerThread.isAlive(), "Consumer thread should be stopped");
	}
}
