package com.xaamxaam.domain.user;

import com.xaamxaam.domain.etablissement.Etablissement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "eleves")
@DiscriminatorValue("ELEVE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Eleve extends Utilisateur {

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_scolaire", nullable = false)
    private NiveauScolaire niveauScolaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;

    @ManyToMany(mappedBy = "eleves")
    @Builder.Default
    private Set<Parent> parents = new HashSet<>();

    public enum NiveauScolaire {
        // College
        SIXIEME, CINQUIEME, QUATRIEME, TROISIEME,
        // Lycee
        SECONDE, PREMIERE, TERMINALE,
        // Superieur (cf. section 3.1 du cahier des charges : "Etudiants du
        // superieur - usage plus avance : methodologie, dissertation, projets")
        LICENCE_1, LICENCE_2, LICENCE_3,
        MASTER_1, MASTER_2,
        DOCTORAT;

        /**
         * Parsing tolerant avec message d'erreur explicite listant les
         * valeurs valides, plutot que le "No enum constant ..." brut de
         * Java (peu comprehensible pour un client de l'API).
         */
        public static NiveauScolaire depuisTexte(String valeur) {
            try {
                return NiveauScolaire.valueOf(valeur.trim().toUpperCase().replace(' ', '_'));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new IllegalArgumentException(
                        "Niveau scolaire invalide : '" + valeur + "'. Valeurs acceptees : "
                                + java.util.Arrays.toString(NiveauScolaire.values())
                );
            }
        }
    }
}
