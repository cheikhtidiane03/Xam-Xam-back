package com.xaamxaam.domain.exercice;

import com.xaamxaam.domain.user.Eleve;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Signalement genere automatiquement par ContournementDetectionService
 * quand une tentative presente un motif suspect (cf. section 4.2 - V2 :
 * "detection de contournement"). Ne bloque jamais l'eleve dans le MVP :
 * sert uniquement a alimenter une file de revue pour le superadmin/enseignant.
 */
@Entity
@Table(name = "signalements_contournement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementContournement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id", nullable = false)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercice_id")
    private Exercice exercice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeSignalement type;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false)
    @Builder.Default
    private boolean traite = false;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    public enum TypeSignalement {
        COPIE_ENONCE, REPONSE_ANORMALEMENT_LONGUE, RAPIDITE_SUSPECTE
    }
}
