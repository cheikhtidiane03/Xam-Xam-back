package com.xaamxaam.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "superadmins")
@DiscriminatorValue("SUPERADMIN")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Superadmin extends Utilisateur {
    // Pas de champs specifiques pour le MVP : le superadmin herite de tous
    // les champs communs d'Utilisateur (nom, email, role, actif...).
    // Pas de @AllArgsConstructor ici : sans champ propre a cette classe,
    // il generait exactement la meme signature que @NoArgsConstructor
    // (Superadmin()), d'ou le conflit "constructor already defined".
}
