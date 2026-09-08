package com.xaamxaam.domain.conformite;

import com.xaamxaam.domain.user.Eleve;
import com.xaamxaam.domain.user.Parent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consentement parental pour un eleve mineur (cf. remarque de securite
 * initiale sur le cahier des charges : conformite protection des donnees
 * pour mineurs). Deux consentements distincts sont trackes separement :
 * - consentementDonnees : traitement general des donnees de l'eleve
 * - consentementStatistiquesNominatives : autorisation explicite pour que
 *   l'enseignant/l'etablissement voit des statistiques NOMINATIVES (nom +
 *   score) plutot que uniquement agregees (cf. section 4.2 du cahier des
 *   charges : "statistiques de classe anonymisees ou nominatives selon
 *   consentement").
 */
@Entity
@Table(name = "consentements_parentaux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentementParental {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleve_id", nullable = false, unique = true)
    private Eleve eleve;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @Column(name = "consentement_donnees", nullable = false)
    @Builder.Default
    private boolean consentementDonnees = false;

    @Column(name = "date_consentement_donnees")
    private LocalDateTime dateConsentementDonnees;

    @Column(name = "consentement_statistiques_nominatives", nullable = false)
    @Builder.Default
    private boolean consentementStatistiquesNominatives = false;

    @Column(name = "date_consentement_statistiques")
    private LocalDateTime dateConsentementStatistiques;

    @Column(name = "date_maj", nullable = false)
    @Builder.Default
    private LocalDateTime dateMaj = LocalDateTime.now();
}
