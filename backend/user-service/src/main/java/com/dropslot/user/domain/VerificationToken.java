package com.dropslot.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "verification_tokens")
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class VerificationToken {
  @Id private UUID id;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String token;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant expiresAt;
}
