package com.dropslot.store.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "dropslot.users", groupId = "dropslot-store")
    public void handleUserEvents(Map<String, Object> event) {
        // Minimal demo: log and ignore unknown payloads. Implement idempotency in production.
        System.out.println("[kafka] received user event: " + event);
    }
}
