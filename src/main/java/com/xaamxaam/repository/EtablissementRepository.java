package com.xaamxaam.repository;

import com.xaamxaam.domain.etablissement.Etablissement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EtablissementRepository extends JpaRepository<Etablissement, UUID> {
    List<Etablissement> findByEnAttenteValidationTrue();
}
