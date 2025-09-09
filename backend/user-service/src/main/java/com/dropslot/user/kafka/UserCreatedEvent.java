package com.dropslot.user.kafka;

import java.time.Instant;

/**
 * Producer-side event record for UserCreated events.
 * This is the complete event structure that gets published to Kafka.
 */
public record UserCreatedEvent(
    String eventType,
    Instant occurredAt,
    String traceId,
    Payload payload
) {
    
    // Factory method for creating UserCreated events
    public static UserCreatedEvent create(String userId, String email) {
        return new UserCreatedEvent(
            "UserCreated",
            Instant.now(),
            null, // traceId can be set later if needed
            new Payload(userId, email, Instant.now())
        );
    }
    
    public record Payload(
        String userId,
        String email,
        Instant createdAt
    ) {}
}
