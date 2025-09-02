package com.dropslot.store.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class StoreDtos {
  public static record CreateStoreRequest(
      @NotBlank String name, @NotBlank String slug, @NotBlank String tenantKey, String logoUrl) {}

  public static record StoreResponse(
      String id, String name, String slug, String tenantKey, String logoUrl) {}

  public static record CreateBranchRequest(
      @NotBlank String name, String address, Double lat, Double lng, String phone) {}

  public static record BranchResponse(
      String id, String name, String address, Double lat, Double lng, String phone) {}

  public static record StoreDetailResponse(StoreResponse store, List<BranchResponse> branches) {}
}
