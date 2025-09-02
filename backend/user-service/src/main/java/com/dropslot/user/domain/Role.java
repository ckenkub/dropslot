package com.dropslot.user.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "roles")
public class Role {
  @Id @GeneratedValue @UuidGenerator private UUID id;

  @Column(nullable = false, unique = true, length = 64)
  private String code;

  @Column(nullable = false, length = 128)
  private String name;
}
