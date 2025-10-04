package com.github.snuk87.keycloak.kafka;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

public class UserDeletionEventListenerProvider implements EventListenerProvider {
    private static final Logger LOG = Logger.getLogger(UserDeletionEventListenerProvider.class);

    private final UserDeletionConsumer consumer;

    public UserDeletionEventListenerProvider(UserDeletionConsumer consumer) {
        this.consumer = consumer;
        LOG.info("UserDeletionEventListenerProvider initialized");
    }

    @Override
    public void onEvent(Event event) {
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
    }

    @Override
    public void close() {
        LOG.info("Shutting down UserDeletionEventListenerProvider");
        consumer.stop();
    }
}