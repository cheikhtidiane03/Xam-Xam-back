package com.xaamxaam.domain.exercice;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Etape finale obligatoire du parcours MVP : l'eleve reformule sa demarche
 * avec ses propres mots (methode Feynman). Le score est calcule par
 * SocratiqueService a partir de l'evaluation IA de la reformulation.
 */
@Entity
@Table(name = "reformulations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reformulation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercice_id", nullable = false, unique = true)
    private Exercice exercice;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String texte;

    /** Score de reformulation sur 100, evalue par l'IA. */
    @Column(nullable = false)
    private int score;

    @Column(name = "feedback_ia", columnDefinition = "TEXT")
    private String feedbackIa;

    @CreationTimestamp
    @Column(name = "date_heure", updatable = false)
    private LocalDateTime dateHeure;
}
