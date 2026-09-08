package com.xaamxaam.repository;

import com.xaamxaam.domain.etablissement.Etablissement;
import com.xaamxaam.domain.user.Eleve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EleveRepository extends JpaRepository<Eleve, UUID> {
    List<Eleve> findByEtablissement(Etablissement etablissement);
}
