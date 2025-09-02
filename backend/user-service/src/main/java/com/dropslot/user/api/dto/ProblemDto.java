package com.dropslot.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Problem JSON (RFC 7807) response")
public record ProblemDto(
    @Schema(example = "https://example.com/probs/out-of-credit") String type,
    @Schema(example = "Out of credit") String title,
    @Schema(example = "Your current balance is 30, but that costs 50.") String detail,
    @Schema(example = "/account/12345/msgs/abc") String instance,
    @Schema(example = "400") Integer status,
    @Schema(description = "Timestamp when the problem occurred", example = "2025-09-02T10:00:00Z")
        Instant timestamp) {}
