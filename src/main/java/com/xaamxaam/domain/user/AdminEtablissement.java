package com.xaamxaam.domain.user;

import com.xaamxaam.domain.etablissement.Etablissement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "admins_etablissement")
@DiscriminatorValue("ADMIN_ETABLISSEMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AdminEtablissement extends Utilisateur {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etablissement_id")
    private Etablissement etablissement;
}
