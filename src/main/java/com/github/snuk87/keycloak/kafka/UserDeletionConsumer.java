package com.github.snuk87.keycloak.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.RealmModel;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UserDeletionConsumer implements Runnable {
    private static final Logger LOG = Logger.getLogger(UserDeletionConsumer.class);

    private final Consumer<String, String> consumer;
    private final KeycloakSessionFactory keycloakSessionFactory;
    private final String realmName;
    private final String deleteUserTopic;
    private volatile boolean running = true;

    public UserDeletionConsumer(KafkaConsumerFactory factory,
                                String clientId,
                                String bootstrapServer,
                                Map<String, Object> optionalProperties,
                                KeycloakSessionFactory session,
                                String realmName,
                                String deleteUserTopic) {
        this.consumer = factory.createConsumer(clientId, bootstrapServer, optionalProperties);
        this.keycloakSessionFactory = session;
        this.realmName = realmName;
        this.deleteUserTopic = deleteUserTopic;
    }

    @Override
    public void run() {
        LOG.info("UserDeletionConsumer started successfully");
        try {
            consumer.subscribe(Collections.singletonList(deleteUserTopic));

            while (running) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000L));

                    if (records.isEmpty()) {
                        continue;
                    }

                    LOG.infof("Received %d message(s) from topic '%s'", records.count(), deleteUserTopic);

                    for (ConsumerRecord<String, String> record : records) {
                        boolean success = processRecord(record);

                        if (success) {
                            Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();
                            offsetsToCommit.put(
                                    new TopicPartition(record.topic(), record.partition()),
                                    new OffsetAndMetadata(record.offset() + 1)
                            );
                            consumer.commitSync(offsetsToCommit);
                            LOG.debugf("Committed offset for partition=%d, offset=%d",
                                    record.partition(), record.offset());
                        } else {
                            LOG.errorf("Failed to process message, will retry: partition=%d, offset=%d",
                                    record.partition(), record.offset());
                        }
                    }

                } catch (WakeupException e) {
                    LOG.info("Wakeup called, exiting consumer loop");
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error("Critical error occurred in UserDeletionConsumer", e);
        } finally {
            try {
                consumer.close();
                LOG.info("UserDeletionConsumer stopped and consumer closed");
            } catch (Exception e) {
                LOG.error("Error closing Kafka consumer", e);
            }
        }
    }

    private boolean processRecord(ConsumerRecord<String, String> record) {
        String userId = record.value();

        LOG.infof("Processing user deletion: userId='%s', partition=%d, offset=%d",
                userId, record.partition(), record.offset());

        try {
            deleteUser(userId);
            return true;
        } catch (Exception e) {
            LOG.errorf(e, "Error processing record: userId='%s'", userId);
            return false;
        }
    }


    private void deleteUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            LOG.warn("Received null or empty userId, skipping deletion");
            return;
        }
        KeycloakSession session = keycloakSessionFactory.create();
        try {
            session.getTransactionManager().begin();
            RealmModel realm = session.realms().getRealmByName(realmName);
            if (realm == null) {
                LOG.errorf("Realm not found: '%s', cannot delete user '%s'", realmName, userId);
                session.getTransactionManager().rollback();
                return;
            }
            session.getContext().setRealm(realm);
            UserModel user = session.users().getUserById(realm, userId);
            if (user != null) {
                String username = user.getUsername();

                boolean deleted = session.users().removeUser(realm, user);
                if (deleted) {
                    LOG.infof("✓ User successfully deleted: userId='%s', username='%s'",
                            userId, username);
                } else {
                    LOG.errorf("✗ Failed to delete user: userId='%s', username='%s'", userId, username);
                }
            } else {
                LOG.warnf("User not found in realm '%s': userId='%s'", realmName, userId);
            }

            session.getTransactionManager().commit();
        } catch (Exception e) {
            try {
                if (session.getTransactionManager().isActive()) {
                    session.getTransactionManager().rollback();
                }
            } catch (Exception ex) {
                LOG.error("Error while rolling back transaction", ex);
            }
            LOG.errorf(e, "Error occurred while deleting user: userId='%s', realm='%s'", userId, realmName);
        } finally {
            try {
                session.close();
            } catch (Exception e) {
                LOG.error("Error closing Keycloak session", e);
            }
        }
    }

    public void stop() {
        LOG.info("Stop signal received for UserDeletionConsumer");
        running = false;
        consumer.wakeup();
    }
}