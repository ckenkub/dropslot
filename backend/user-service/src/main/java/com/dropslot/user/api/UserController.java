package com.dropslot.user.api;

import com.dropslot.user.api.dto.UserProfileDto;
import com.dropslot.user.domain.User;
import com.dropslot.user.repo.UserRepository;
import com.dropslot.user.service.AuthService;
import com.dropslot.user.util.LogUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {
  private final UserRepository userRepository;
  private final AuthService authService;
  private static final Logger log = LoggerFactory.getLogger(UserController.class);

  @GetMapping("/me")
  @Operation(summary = "Get current user's profile")
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized",
      content =
          @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
  public ResponseEntity<UserProfileDto> me(Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());
    LogUtils.putUserContext(userId.toString());
    try {
      log.debug("Get profile");
      User user = userRepository.findById(userId).orElseThrow();
      return ResponseEntity.ok(authService.toProfile(user));
    } finally {
      LogUtils.removeUserContext();
    }
  }

  @PutMapping("/me")
  @Operation(summary = "Update current user's profile")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request",
      content =
          @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized",
      content =
          @Content(schema = @Schema(implementation = com.dropslot.user.api.dto.ProblemDto.class)))
  public ResponseEntity<UserProfileDto> updateMe(
      Authentication authentication, @RequestBody UserProfileDto body) {
    UUID userId = UUID.fromString(authentication.getName());
    LogUtils.putUserContext(userId.toString());
    try {
      log.info("Update profile");
      User user = userRepository.findById(userId).orElseThrow();
      if (body.name() != null) user.setName(body.name());
      userRepository.save(user);
      return ResponseEntity.ok(authService.toProfile(user));
    } finally {
      LogUtils.removeUserContext();
    }
  }
}
