package com.dropslot.store.service;

import com.dropslot.store.api.dto.StoreDtos;
import com.dropslot.store.domain.Branch;
import com.dropslot.store.domain.Store;
import com.dropslot.store.repo.BranchRepository;
import com.dropslot.store.repo.StoreRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {
  private final StoreRepository storeRepository;
  private final BranchRepository branchRepository;

  @Transactional
  public StoreDtos.StoreResponse create(StoreDtos.CreateStoreRequest req, UUID creatorId) {
    if (storeRepository.existsBySlug(req.slug())) {
      throw new IllegalArgumentException("Slug already exists");
    }
    Store store =
        Store.builder()
            .name(req.name())
            .slug(req.slug())
            .tenantKey(req.tenantKey())
            .logoUrl(req.logoUrl())
            .createdBy(creatorId)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    store = storeRepository.save(store);
    return toResponse(store);
  }

  @Transactional(readOnly = true)
  public StoreDtos.StoreDetailResponse get(UUID id) {
    Store store = storeRepository.findById(id).orElseThrow();
    List<Branch> branches = branchRepository.findByStore(store);
    return new StoreDtos.StoreDetailResponse(
        toResponse(store), branches.stream().map(this::toResponse).toList());
  }

  @Transactional
  public StoreDtos.StoreResponse update(UUID id, StoreDtos.CreateStoreRequest req) {
    Store store = storeRepository.findById(id).orElseThrow();
    store.setName(req.name());
    store.setLogoUrl(req.logoUrl());
    store.setUpdatedAt(Instant.now());
    store = storeRepository.save(store);
    return toResponse(store);
  }

  @Transactional
  public StoreDtos.BranchResponse addBranch(UUID storeId, StoreDtos.CreateBranchRequest req) {
    Store store = storeRepository.findById(storeId).orElseThrow();
    Branch b =
        Branch.builder()
            .store(store)
            .name(req.name())
            .address(req.address())
            .lat(req.lat())
            .lng(req.lng())
            .phone(req.phone())
            .build();
    b = branchRepository.save(b);
    return toResponse(b);
  }

  private StoreDtos.StoreResponse toResponse(Store s) {
    return new StoreDtos.StoreResponse(
        s.getId() != null ? s.getId().toString() : null,
        s.getName(),
        s.getSlug(),
        s.getTenantKey(),
        s.getLogoUrl());
  }

  private StoreDtos.BranchResponse toResponse(Branch b) {
    return new StoreDtos.BranchResponse(
        b.getId() != null ? b.getId().toString() : null,
        b.getName(),
        b.getAddress(),
        b.getLat(),
        b.getLng(),
        b.getPhone());
  }
}
