package com.dropslot.user.api.dto;

import java.util.Set;

public record UserProfileDto(String id, String email, String name, Set<String> roles) {}
