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
    String key = (event.getPayload() != null && event.getPayload().userId != null)
        ? event.getPayload().userId : "";
    kafkaTemplate.send("dropslot.users", key, event);
    }
}
