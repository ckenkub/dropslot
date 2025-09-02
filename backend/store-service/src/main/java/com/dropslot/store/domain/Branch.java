package com.dropslot.store.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "branches")
public class Branch {
  @Id @GeneratedValue @UuidGenerator private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @Column(nullable = false)
  private String name;

  private String address;

  private Double lat;

  private Double lng;

  private String phone;

  @Column(columnDefinition = "json")
  private String openingHours;
}
