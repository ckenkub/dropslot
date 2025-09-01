package com.dropslot.user.repo;

import com.dropslot.user.domain.VerificationToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
  Optional<VerificationToken> findByEmailAndType(String email, String type);

  Optional<VerificationToken> findByTokenAndType(String token, String type);

  void deleteByEmailAndType(String email, String type);
}
