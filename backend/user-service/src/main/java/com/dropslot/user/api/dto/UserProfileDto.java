package com.dropslot.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(description = "User profile information")
public record UserProfileDto(
    @Schema(example = "a7f3c3d6-...") String id,
    @Schema(example = "user@example.com") String email,
    @Schema(example = "Full Name") String name,
    @Schema(description = "Set of role codes") Set<String> roles) {}
