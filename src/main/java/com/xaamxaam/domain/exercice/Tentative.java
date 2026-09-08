package com.xaamxaam.domain.exercice;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tentatives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tentative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercice_id", nullable = false)
    private Exercice exercice;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    /**
     * Reponse socratique generee par SocratiqueService pour cette tentative
     * (la question de relance envoyee a l'eleve, jamais la reponse finale).
     */
    @Column(name = "relance_ia", columnDefinition = "TEXT")
    private String relanceIa;

    /**
     * Niveau d'indice actif au moment de la tentative (0 = aucun indice
     * encore debloque, 1 a 3 = niveaux d'indices progressifs).
     */
    @Column(name = "indice_niveau", nullable = false)
    @Builder.Default
    private int indiceNiveau = 0;

    @CreationTimestamp
    @Column(name = "date_heure", updatable = false)
    private LocalDateTime dateHeure;
}
