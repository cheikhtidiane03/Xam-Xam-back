package com.xaamxaam.domain.exercice;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String description;

    /** Seuil declenchant le badge (nombre d'exercices, score...) selon le code. */
    private Integer seuil;
}
