package com.dropslot.user.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserCreated(UserCreatedEvent event) {
        String key = (event.payload() != null && event.payload().userId() != null)
            ? event.payload().userId() : "";
        kafkaTemplate.send("dropslot.users", key, event);
    }
}
