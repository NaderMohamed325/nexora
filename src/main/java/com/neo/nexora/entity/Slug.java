package com.neo.nexora.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Entity
@Table(name = "slugs")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Slug {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    @Column(nullable = false)
    private Long clickCount = 0L;

}
