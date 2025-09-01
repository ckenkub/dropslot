package com.dropslot.user.api.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserProfileDto user
) {}
