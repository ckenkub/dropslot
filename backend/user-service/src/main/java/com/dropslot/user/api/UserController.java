package com.dropslot.user.api;

import com.dropslot.user.api.dto.UserProfileDto;
import com.dropslot.user.domain.User;
import com.dropslot.user.repo.UserRepository;
import com.dropslot.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(authService.toProfile(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateMe(Authentication authentication, @RequestBody UserProfileDto body) {
        UUID userId = UUID.fromString(authentication.getName());
        User user = userRepository.findById(userId).orElseThrow();
    if (body.name() != null) user.setName(body.name());
        userRepository.save(user);
        return ResponseEntity.ok(authService.toProfile(user));
    }
}
