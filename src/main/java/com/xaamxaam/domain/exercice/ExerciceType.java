package com.xaamxaam.domain.exercice;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.user.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modele de la bibliotheque d'exercices types (cf. section 4.2 du cahier
 * des charges - V2). Un exercice type sert de "template" que l'eleve peut
 * importer pour creer son propre Exercice de travail.
 */
@Entity
@Table(name = "exercices_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciceType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String enonce;

    @Column(nullable = false)
    private String matiere;

    @Column(nullable = false)
    private String niveau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_id")
    private Utilisateur creePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;
}
