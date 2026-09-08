package com.xaamxaam.domain.abonnement;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.user.Parent;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "abonnements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAbonnement type;

    /** Renseigne uniquement si type = PARENT. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    /** Renseigne uniquement si type = ETABLISSEMENT. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutAbonnement statut = StatutAbonnement.EN_ATTENTE;

    /** Montant en FCFA, cf. section 9 du cahier des charges. */
    @Column(name = "montant_fcfa")
    private Long montantFcfa;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    public enum TypeAbonnement {
        PARENT, ETABLISSEMENT
    }

    public enum StatutAbonnement {
        ACTIF, EXPIRE, EN_ATTENTE, ANNULE
    }
}
