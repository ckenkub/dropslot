package com.dropslot.store.api;

import com.dropslot.store.api.dto.StoreDtos;
import com.dropslot.store.service.StoreService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {
  private final StoreService storeService;

  @PostMapping
  public ResponseEntity<StoreDtos.StoreResponse> create(
      @Valid @RequestBody StoreDtos.CreateStoreRequest req,
      @RequestHeader(value = "X-User-Id", required = false) String userId) {
    UUID creator = userId != null ? UUID.fromString(userId) : null;
    return ResponseEntity.ok(storeService.create(req, creator));
  }

  @GetMapping("/{id}")
  public ResponseEntity<StoreDtos.StoreDetailResponse> get(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(storeService.get(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<StoreDtos.StoreResponse> update(
      @PathVariable("id") UUID id, @Valid @RequestBody StoreDtos.CreateStoreRequest req) {
    return ResponseEntity.ok(storeService.update(id, req));
  }

  @PostMapping("/{id}/branches")
  public ResponseEntity<StoreDtos.BranchResponse> addBranch(
      @PathVariable("id") UUID id, @Valid @RequestBody StoreDtos.CreateBranchRequest req) {
    return ResponseEntity.ok(storeService.addBranch(id, req));
  }
}
