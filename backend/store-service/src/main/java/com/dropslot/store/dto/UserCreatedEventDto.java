package com.dropslot.store.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Store service's view of a user created event.
 * Only contains fields that the store service cares about.
 * JsonIgnoreProperties ensures we ignore unknown fields from producer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserCreatedEventDto(
    String eventType,
    PayloadDto payload
) {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PayloadDto(
        String userId,
        String email
        // We ignore createdAt, traceId, and other fields we don't need
    ) {}
}
