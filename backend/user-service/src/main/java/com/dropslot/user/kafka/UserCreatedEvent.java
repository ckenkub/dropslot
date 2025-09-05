package com.dropslot.user.kafka;

import java.time.Instant;

public class UserCreatedEvent {
    private String eventType = "UserCreated";
    private Instant occurredAt = Instant.now();
    private String traceId;

    private Payload payload;

    public static class Payload {
        public String userId;
        public String email;
        public Instant createdAt;
    }

    // getters/setters
    public String getEventType() { return eventType; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Payload getPayload() { return payload; }
    public void setPayload(Payload payload) { this.payload = payload; }
}
