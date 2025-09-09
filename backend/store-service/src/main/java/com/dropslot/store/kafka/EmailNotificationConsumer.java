package com.dropslot.store.kafka;

import com.dropslot.store.dto.UserCreatedEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationConsumer.class);

    @KafkaListener(
        topics = "dropslot.users", 
        groupId = "dropslot-email",  // 🆕 NEW CONSUMER GROUP!
        containerFactory = "emailKafkaListenerContainerFactory" // 🎯 Uses earliest offset!
    )
    public void handleUserEvents(@Payload UserCreatedEventDto event, 
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            logger.info("📧 EMAIL SERVICE - Received event from topic {}: type={}", topic, event.eventType());
            
            if ("UserCreated".equals(event.eventType())) {
                sendWelcomeEmail(event);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process email event: {}", event, e);
        }
    }
    
    private void sendWelcomeEmail(UserCreatedEventDto event) {
        if (event.payload() != null) {
            String userId = event.payload().userId();
            String email = event.payload().email();
            
            logger.info("📨 Sending welcome email to: {} (userId={})", email, userId);
            
            // TODO: Implement actual email sending logic
            // emailService.sendWelcomeEmail(email, userId);
        }
    }
}
