package com.xaamxaam.domain.user;

import com.xaamxaam.domain.etablissement.Etablissement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "enseignants")
@DiscriminatorValue("ENSEIGNANT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Enseignant extends Utilisateur {

    @Column(nullable = false)
    private String matiere;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;
}
