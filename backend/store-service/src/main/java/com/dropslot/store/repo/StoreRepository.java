package com.dropslot.store.repo;

import com.dropslot.store.domain.Store;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, UUID> {
  Optional<Store> findBySlug(String slug);

  boolean existsBySlug(String slug);
}
