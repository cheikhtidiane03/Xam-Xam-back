package com.xaamxaam.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Code temporaire genere par un Eleve pour rattacher son compte a celui
 * d'un Parent. Choix de conception deliberre : le lien se fait TOUJOURS a
 * l'initiative de l'eleve (qui genere et partage le code), jamais a
 * l'initiative du parent via l'email de l'eleve - ce qui empecherait un
 * inconnu de se rattacher au compte d'un mineur juste en devinant/connaissant
 * son adresse email (cf. exigences de protection des donnees des mineurs).
 */
@Entity
@Table(name = "codes_liaison_parent_eleve")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeLiaisonParentEleve {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(nullable = false)
    @Builder.Default
    private boolean utilise = false;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;
}
