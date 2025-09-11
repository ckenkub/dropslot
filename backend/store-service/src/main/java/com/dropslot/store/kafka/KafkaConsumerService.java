package com.dropslot.store.kafka;

import com.dropslot.store.dto.UserCreatedEventDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    
    // 📊 Metrics for monitoring
    private final Counter messagesProcessed;
    private final Counter messagesError;
    private final Timer processingTime;

    public KafkaConsumerService(MeterRegistry meterRegistry) {
        this.messagesProcessed = Counter.builder("kafka.messages.processed")
                .tag("consumer", "store-service")
                .tag("topic", "dropslot.users")
                .description("Total messages processed successfully")
                .register(meterRegistry);
                
        this.messagesError = Counter.builder("kafka.messages.error")
                .tag("consumer", "store-service")
                .tag("topic", "dropslot.users")
                .description("Total messages that failed processing")
                .register(meterRegistry);
                
        this.processingTime = Timer.builder("kafka.processing.duration")
                .tag("consumer", "store-service")
                .description("Time taken to process messages")
                .register(meterRegistry);
    }

    @KafkaListener(
        topics = "dropslot.users", 
        groupId = "dropslot-store",
        containerFactory = "storeKafkaListenerContainerFactory" // 🎯 Uses store-specific factory
    )
    public void handleUserEvents(@Payload UserCreatedEventDto event, 
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                @Header(KafkaHeaders.OFFSET) long offset,
                                Acknowledgment acknowledgment) {
        
        Instant startTime = Instant.now();
        
        try {
            // Add correlation ID for tracing
            MDC.put("kafkaEvent", String.format("%s-%d-%d", topic, partition, offset));
            
            logger.info("🏪 STORE SERVICE - Processing event from topic {}, partition {}, offset {}: type={}", 
                       topic, partition, offset, event.eventType());
            
            if ("UserCreated".equals(event.eventType())) {
                handleUserCreatedWithRetry(event);
                messagesProcessed.increment();
                logger.info("✅ Successfully processed event: {}", event.eventType());
            } else {
                logger.warn("⚠️ Unknown event type: {}", event.eventType());
            }
            
            // Manual acknowledgment for better control
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            messagesError.increment();
            logger.error("❌ Failed to process user event from partition {}, offset {}: {}", 
                        partition, offset, event, e);
            
            // In production, you might want to send to dead letter queue
            // For now, we acknowledge to avoid infinite retries
            acknowledgment.acknowledge();
            
        } finally {
            processingTime.record(java.time.Duration.between(startTime, Instant.now()));
            MDC.clear();
        }
    }
    
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void handleUserCreatedWithRetry(UserCreatedEventDto event) {
        if (event.payload() != null) {
            String userId = event.payload().userId();
            String email = event.payload().email();
            
            logger.info("✅ SUCCESS: Processing user creation: userId={}, email={}", userId, email);
            
            if (userId != null && !userId.isEmpty()) {
                createDefaultStoreForUser(userId, email);
            }
        } else {
            logger.warn("Received UserCreated event with null payload");
        }
    }
    
    private void createDefaultStoreForUser(String userId, String email) {
        logger.info("🏪 Creating default store for user: {} ({})", userId, email);
        // TODO: Implement store creation logic
        // - Create a default store for the user
        // - Set up initial store configuration  
        // - Send welcome notification
    }
}
