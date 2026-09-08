package com.xaamxaam.domain.user;

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
@Table(name = "parents")
@DiscriminatorValue("PARENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Parent extends Utilisateur {

    @ManyToMany
    @JoinTable(
            name = "parent_eleve",
            joinColumns = @JoinColumn(name = "parent_id"),
            inverseJoinColumns = @JoinColumn(name = "eleve_id")
    )
    @Builder.Default
    private Set<Eleve> eleves = new HashSet<>();
}
