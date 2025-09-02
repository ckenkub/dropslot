package com.dropslot.store.repo;

import com.dropslot.store.domain.Branch;
import com.dropslot.store.domain.Store;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
  List<Branch> findByStore(Store store);
}
