package com.xaamxaam.repository;

import com.xaamxaam.domain.exercice.SignalementContournement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SignalementContournementRepository extends JpaRepository<SignalementContournement, UUID> {
    List<SignalementContournement> findByTraiteFalseOrderByDateCreationDesc();
    List<SignalementContournement> findByEleveIdOrderByDateCreationDesc(UUID eleveId);
}
